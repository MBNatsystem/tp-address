## Recommandation

Pour ce fichier, je recommande :

> **un `JsonItemReader` Spring Batch utilisant un `JsonObjectReader` personnalisé basé sur le parser streaming Jackson**, afin de positionner le flux sur le tableau `features`.

Vous conservez ainsi :

* une consommation mémoire limitée à une commune ;
* le modèle chunk-oriented de Spring Batch ;
* la sauvegarde de la position dans l’`ExecutionContext` ;
* la possibilité de redémarrer le step ;
* aucune transformation préalable du fichier.

Le `JsonItemReader` standard ne peut pas lire directement votre fichier : il attend un tableau à la racine, sous la forme `[{...}, {...}]`, alors que le GeoJSON possède un objet racine `FeatureCollection` contenant le tableau `features`. Il est par ailleurs non thread-safe. ([Home][1])

## Méthodes possibles

| Méthode                                   | Avantages                                                 | Inconvénients                                   | Avis                                    |
| ----------------------------------------- | --------------------------------------------------------- | ----------------------------------------------- | --------------------------------------- |
| Désérialiser toute la `FeatureCollection` | Très peu de code                                          | Toutes les géométries sont chargées en mémoire  | À éviter pour les communes françaises   |
| Transformer le fichier en tableau JSON    | Compatible directement avec `JsonItemReader`              | Fichier intermédiaire et lecture supplémentaire | Correct                                 |
| Transformer en NDJSON                     | Facile à lire ligne par ligne, partitionnable             | Prétraitement obligatoire                       | Très bon si vous contrôlez l’import     |
| Parser Jackson streaming personnalisé     | Faible mémoire, lecture directe, intégré au restart Batch | Petit lecteur à développer                      | **Recommandé**                          |
| `FeatureJSON` de GeoTools                 | Comprend nativement GeoJSON et JTS                        | Dépendances GeoTools importantes                | À utiliser si GeoTools est déjà présent |
| Importer d’abord dans PostGIS             | Excellent pour requêtes géographiques                     | Infrastructure supplémentaire                   | Pertinent si la destination est PostGIS |

GeoTools propose bien `streamFeatureCollection`, alors que `readFeatureCollection` charge explicitement toute la collection en mémoire. ([docs.geotools.org][2])

Spring Batch 6 utilise Jackson 3 par défaut. Les imports Jackson sont donc désormais sous `tools.jackson.*`, et une grande partie des API infrastructure de Spring Batch se trouve sous `org.springframework.batch.infrastructure.*`. ([Home][3])

---

# Solution recommandée

## 1. Modèle Java

Je garderais initialement la géométrie sous forme de `JsonNode`. Cela permet de gérer sans difficulté les `Polygon`, `MultiPolygon` et d’éventuels autres types.

```java
import tools.jackson.databind.JsonNode;

public record AdministrativeFeature(
        String type,
        AdministrativeProperties properties,
        JsonNode geometry
) {
}

public record AdministrativeProperties(
        String code,
        String nom,
        String departement,
        String region,
        String epci
) {
}
```

Il ne faut surtout pas déclarer directement :

```java
List<List<List<double[]>>> coordinates
```

car cette structure ne convient qu’à un `Polygon`. Un `MultiPolygon` possède un niveau de tableau supplémentaire. GeoJSON définit également l’ordre des coordonnées comme longitude puis latitude. ([IETF Datatracker][4])

## 2. Adaptateur pour le tableau `features`

```java
import java.io.IOException;

import org.springframework.batch.infrastructure.item.json.JsonObjectReader;
import org.springframework.core.io.Resource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

public final class FeatureCollectionJsonObjectReader<T>
        implements JsonObjectReader<T> {

    private final JsonMapper mapper;
    private final ObjectReader objectReader;

    private JsonParser parser;
    private boolean finished;

    public FeatureCollectionJsonObjectReader(
            JsonMapper mapper,
            Class<T> itemType
    ) {
        this.mapper = mapper;
        this.objectReader = mapper.readerFor(itemType);
    }

    @Override
    public void open(Resource resource) throws Exception {
        parser = mapper.createParser(resource.getInputStream());
        finished = false;
        positionOnFeaturesArray();
    }

    private void positionOnFeaturesArray() throws Exception {
        JsonToken token;

        while ((token = parser.nextToken()) != null) {
            if (token == JsonToken.PROPERTY_NAME
                    && "features".equals(parser.currentName())) {

                JsonToken valueToken = parser.nextToken();

                if (valueToken != JsonToken.START_ARRAY) {
                    throw new IOException(
                            "La propriété 'features' n'est pas un tableau JSON"
                    );
                }

                return;
            }
        }

        throw new IOException(
                "Aucune propriété 'features' trouvée dans le GeoJSON"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read() throws Exception {
        if (finished) {
            return null;
        }

        JsonToken token = parser.nextToken();

        if (token == null || token == JsonToken.END_ARRAY) {
            finished = true;
            return null;
        }

        if (token != JsonToken.START_OBJECT) {
            throw new IOException(
                    "Une Feature était attendue, token rencontré : " + token
            );
        }

        return (T) objectReader.readValue(parser);
    }

    @Override
    public void jumpToItem(int itemIndex) throws Exception {
        /*
         * Appelé par Spring Batch lors d'un restart.
         * Le parser vient d'être rouvert et repositionné au début
         * du tableau features.
         */
        for (int index = 0; index < itemIndex; index++) {
            if (read() == null) {
                throw new IOException(
                        "Impossible d'atteindre la Feature d'index " + itemIndex
                );
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (parser != null) {
            parser.close();
            parser = null;
        }

        finished = true;
    }
}
```

Jackson 3 utilise notamment `JsonToken.PROPERTY_NAME`, qui remplace le `FIELD_NAME` de Jackson 2. ([javadoc][5])

## 3. Configuration du reader Spring Batch

```java
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.json.JsonItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;

import tools.jackson.databind.json.JsonMapper;

@Bean
@StepScope
public JsonItemReader<AdministrativeFeature> administrativeFeatureReader(
        @Value("#{jobParameters['inputFile']}") String inputFile
) {
    JsonMapper mapper = JsonMapper.builder().build();

    var objectReader =
            new FeatureCollectionJsonObjectReader<>(
                    mapper,
                    AdministrativeFeature.class
            );

    var reader = new JsonItemReader<>(
            new FileSystemResource(inputFile),
            objectReader
    );

    reader.setName("administrativeFeatureReader");

    return reader;
}
```

Le `JsonItemReader` hérite d’`AbstractItemCountingItemStreamItemReader` et sauvegarde son nombre d’éléments lus. Lors d’un restart, il appelle `jumpToItem` pour retrouver la Feature précédemment atteinte. ([Home][6])

## 4. Step

```java
@Bean
public Step importAdministrativeFeaturesStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        JsonItemReader<AdministrativeFeature> administrativeFeatureReader,
        ItemProcessor<AdministrativeFeature, AdministrativeArea> processor,
        ItemWriter<AdministrativeArea> writer
) {
    return new StepBuilder(
            "importAdministrativeFeaturesStep",
            jobRepository
    )
            .<AdministrativeFeature, AdministrativeArea>chunk(
                    100,
                    transactionManager
            )
            .reader(administrativeFeatureReader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

Une taille de chunk autour de `50` à `200` constitue un point de départ raisonnable. Chaque Feature peut contenir beaucoup de coordonnées ; il vaut mieux éviter des chunks de plusieurs milliers d’objets.

---

# Gestion de la géométrie

## Lorsque vous n’effectuez pas d’opération géographique

Gardez simplement la géométrie en :

* `JsonNode` ;
* chaîne GeoJSON ;
* colonne PostgreSQL `jsonb`.

C’est l’option la plus légère.

## Lorsque vous devez faire des opérations spatiales

Convertissez la géométrie en `org.locationtech.jts.geom.Geometry` dans l’`ItemProcessor`.

```java
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import tools.jackson.databind.json.JsonMapper;

public final class AdministrativeFeatureProcessor
        implements ItemProcessor<AdministrativeFeature, AdministrativeArea> {

    private final JsonMapper mapper;
    private final GeoJsonReader geoJsonReader;

    public AdministrativeFeatureProcessor(JsonMapper mapper) {
        this.mapper = mapper;

        GeometryFactory geometryFactory =
                new GeometryFactory(
                        new PrecisionModel(),
                        4326
                );

        this.geoJsonReader = new GeoJsonReader(geometryFactory);
    }

    @Override
    public AdministrativeArea process(
            AdministrativeFeature feature
    ) throws Exception {

        Geometry geometry = geoJsonReader.read(
                mapper.writeValueAsString(feature.geometry())
        );

        return new AdministrativeArea(
                feature.properties().code(),
                feature.properties().nom(),
                feature.properties().departement(),
                feature.properties().region(),
                feature.properties().epci(),
                geometry
        );
    }
}
```

Le module `jts-io-common` fournit officiellement `GeoJsonReader`, qui transforme un fragment de géométrie GeoJSON en `Geometry`. ([LocationTech][7])

Dépendance Maven indicative :

```xml
<dependency>
    <groupId>org.locationtech.jts.io</groupId>
    <artifactId>jts-io-common</artifactId>
    <version>1.20.0</version>
</dependency>
```

Lorsque vous utilisez Spring Boot, laissez de préférence son BOM gérer les versions Jackson et Spring Batch plutôt que de les fixer indépendamment.

---

# Variante NDJSON

Lorsque vous pouvez ajouter une étape de préparation, une autre excellente architecture consiste à produire :

```json
{"type":"Feature","properties":{...},"geometry":{...}}
{"type":"Feature","properties":{...},"geometry":{...}}
{"type":"Feature","properties":{...},"geometry":{...}}
```

Puis à utiliser un `FlatFileItemReader`, une Feature par ligne.

Cette approche simplifie :

* les redémarrages par numéro de ligne ;
* les rejets vers un fichier ;
* le découpage en plusieurs fichiers ;
* le partitionnement ;
* l’inspection manuelle.

Elle implique cependant de lire le fichier une première fois pour produire le NDJSON. Il ne faut pas utiliser directement un `FlatFileItemReader` sur le GeoJSON actuel : votre exemple est une `FeatureCollection`, potentiellement entièrement écrite sur une seule ligne.

## Choix final

Pour un seul batch séquentiel important directement le fichier data.gouv.fr :

> **`JsonItemReader` + `FeatureCollectionJsonObjectReader` streaming Jackson + géométrie conservée en `JsonNode`, puis convertie en JTS dans le processor uniquement si nécessaire.**

GeoTools devient préférable seulement si votre application utilise déjà son modèle `SimpleFeature`, les systèmes de coordonnées ou ses autres fonctions SIG. Le jeu data.gouv est disponible en GeoJSON et en GeoJSON compressé `.gz` ; pour un fichier compressé lu directement, il faudra ouvrir le flux avec un `GZIPInputStream` dans le lecteur personnalisé. ([data.gouv.fr][8])

[1]: https://docs.spring.io/spring-batch/reference/readers-and-writers/json-reading-writing.html "JSON Item Readers And Writers :: Spring Batch Reference"
[2]: https://docs.geotools.org/latest/javadocs/org/geotools/geojson/feature/FeatureJSON.html?utm_source=chatgpt.com "FeatureJSON (Geotools modules 36-SNAPSHOT API)"
[3]: https://docs.spring.io/spring-batch/reference/whatsnew.html "What’s new in Spring Batch 6 :: Spring Batch Reference"
[4]: https://datatracker.ietf.org/doc/html/rfc7946?utm_source=chatgpt.com "RFC 7946 - The GeoJSON Format"
[5]: https://javadoc.io/static/tools.jackson.core/jackson-core/3.0.0-rc9/tools.jackson.core/tools/jackson/core/JsonParser.html?utm_source=chatgpt.com "JsonParser (Jackson-core 3.0.0-rc9 API)"
[6]: https://docs.spring.io/spring-batch/reference/api/org/springframework/batch/infrastructure/item/json/JacksonJsonObjectReader.html "JacksonJsonObjectReader (Spring Batch 6.0.4 API)"
[7]: https://locationtech.github.io/jts/javadoc-io/org/locationtech/jts/io/geojson/GeoJsonReader.html?utm_source=chatgpt.com "Class GeoJsonReader"
[8]: https://www.data.gouv.fr/datasets/contours-administratifs?utm_source=chatgpt.com "Jeu de données - Contours administratifs"
