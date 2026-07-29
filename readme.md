# TP Adresse - Import BAN

## Presentation

Application Spring Batch permettant d'importer les donnees de la Base Adresse Nationale (BAN) dans une base SQLite.

Technologies :

* Spring Boot
* Spring Batch
* SQLite

---

## Prerequis

* Java 25
* Maven

---

## Configuration

Les principaux parametres sont definis dans `application.yaml` :

```yaml
batch:
  address:
    input-file: file:./data/csv/adresses-79.csv
    chunk-size: 500
```

* `input-file` : fichier CSV BAN a importer
* `chunk-size` : taille des lots traites par Spring Batch

Le fichier CSV doit être place dans :

```text
data/csv
```

Exemple :

```text
data/csv/adresses-79.csv
```

---

## Compilation

```bash
mvn clean package
```

---

## Execution

### Import complet

```bash
mvn spring-boot:run
```

```bash
java -jar .\target\tp-adresse-test-0.0.1-SNAPSHOT.jar
```

### Import par code postal

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=codePostal=79240"
```

```bash
java -jar .\target\tp-adresse-test-0.0.1-SNAPSHOT.jar codePostal=79240
```

### Import par code INSEE

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=codeInsee=79002"
```

```bash
java -jar .\target\tp-adresse-test-0.0.1-SNAPSHOT.jar codeInsee=79002
```

### Import par code postal et code INSEE

```bash
mvn spring-boot:run "-Dspring-boot.run.arguments=codePostal=79240 codeInsee=79002"
```

---

## Base de donnees

La base SQLite est creee automatiquement dans :

```text
data/adresses.db
```

La table: ban_address_final
Correspond au resultat de l'import (cependant les conflits metiers ne seront pas importe)

La table: address_reject
Contient toutes les erreurs de dupplication ou de conflit metier. Les duplications ont ete insere mais pas les conflits metiers

La table: address_sync_plan
Contient les informations de votre dernier import (lignes ajoutees/modifiees/supprimees)

---

## Configuration avancee

L'application utilise SQLite. Certains parametres peuvent être ajustes pour privilegier soit la performance, soit la securite des donnees.

Ces parametres sont particulierement importants lors de l'import de fichiers volumineux. Ils sont a modifier

#### `PRAGMA journal_mode`

Definit le mode de journalisation utilise par SQLite.

| Valeur   | Performance |    Securite | Usage conseille                        |
| -------- | ----------: | ----------: | -------------------------------------- |
| `DELETE` |     Moyenne |      elevee | Mode classique SQLite                  |
| `WAL`    |      elevee |      elevee | Recommande pour les imports volumineux |
| `OFF`    | Tres elevee | Tres faible | Deconseille                            |

Valeur recommandee :

```sql
PRAGMA journal_mode = WAL;
```

Le mode `WAL` ecrit les changements dans un fichier separe avant de les integrer a la base principale. Il est generalement plus performant pour les ecritures importantes.

#### `PRAGMA synchronous`

Definit le niveau de synchronisation des ecritures sur le disque.

| Valeur   | Performance | Securite | Usage conseille           |
| -------- | ----------: | -------: | ------------------------- |
| `FULL`   |  Plus lente | Maximale | Environnement sensible    |
| `NORMAL` |       Bonne |    Bonne | Recommande pour ce projet |
| `OFF`    | Tres rapide |   Faible | Deconseille               |

Valeur recommandee pour un bon compromis :

```sql
PRAGMA synchronous = NORMAL;
```

Avec `NORMAL`, SQLite effectue moins de synchronisations disque qu'avec `FULL`. Cela ameliore les performances, avec un risque limite en cas d'arrêt brutal de la machine pendant l'import.

#### `PRAGMA cache_size`

Definit la quantite de memoire utilisee par SQLite pour mettre en cache les pages de la base.

Une valeur negative indique une taille en kibioctets.

Exemples :

```sql
PRAGMA cache_size = -65536;   -- environ 64 Mo
PRAGMA cache_size = -262144;  -- environ 256 Mo
PRAGMA cache_size = -524288;  -- environ 512 Mo
```

| Valeur    | Performance | Memoire utilisee |
| --------- | ----------: | ---------------: |
| `-65536`  |    Correcte |           Faible |
| `-262144` |       Bonne |          Moyenne |
| `-524288` |  Tres bonne |           elevee |

Valeur recommandee :

```sql
PRAGMA cache_size = -262144;
```

Augmenter cette valeur peut ameliorer les performances sur les gros imports, mais augmente la consommation memoire.

#### `PRAGMA temp_store`

Definit où SQLite stocke les structures temporaires utilisees pendant certains tris, jointures ou regroupements.

| Valeur   | Performance | Memoire utilisee | Usage conseille                        |
| -------- | ----------: | ---------------: | -------------------------------------- |
| `FILE`   |  Plus lente |           Faible | Machines avec peu de RAM               |
| `MEMORY` | Plus rapide |      Plus elevee | Recommande pour les imports volumineux |

Valeur recommandee :

```sql
PRAGMA temp_store = MEMORY;
```

Cette option permet de limiter les acces disque pendant les traitements SQL complexes.

#### Configuration rapide

Configuration recommandee pour privilegier les performances pendant l'import :

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = -262144;
PRAGMA temp_store = MEMORY;
```

Cette configuration est adaptee a un import volumineux sur une machine stable, idealement avec un SSD.

#### Configuration securisee

Configuration recommandee si la securite des ecritures est prioritaire :

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA cache_size = -65536;
PRAGMA temp_store = FILE;
```

Cette configuration est plus prudente, mais moins rapide.

---

### Execution avancee

Pour les imports volumineux, il est possible d'augmenter la memoire allouee a la JVM.

Exemple :

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xms2g -Xmx4g"
```

```bash
java "-Dspring-boot.run.jvmArguments=-Xms2g -Xmx4g" -jar .\target\tp-adresse-test-0.0.1-SNAPSHOT.jar
```

Parametres :

| Parametre | Description                                   |
| --------- | --------------------------------------------- |
| `-Xms2g`  | Memoire initiale allouee a la JVM : 2 Go      |
| `-Xmx4g`  | Memoire maximale autorisee pour la JVM : 4 Go |

Exemple avec plus de memoire :

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xms4g -Xmx8g"
```

Recommandations :

| Taille du fichier | Memoire JVM recommandee |
| ----------------- | ----------------------- |
| Petit fichier     | `-Xms512m -Xmx1g`       |
| Fichier moyen     | `-Xms1g -Xmx2g`         |
| Gros fichier      | `-Xms2g -Xmx4g`         |
| Tres gros fichier | `-Xms4g -Xmx8g`         |

Augmenter la memoire JVM peut ameliorer la stabilite du traitement sur les gros volumes, mais ne remplace pas les optimisations SQLite.

Pour de meilleures performances, il est recommande d'executer l'import sur un SSD.
