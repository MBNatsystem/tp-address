# TP Adresse - Import BAN

## Présentation

Application Spring Batch permettant d'importer les données de la Base Adresse Nationale (BAN) dans une base SQLite.

Technologies :

* Spring Boot
* Spring Batch
* SQLite

---

## Prérequis

* Java 25
* Maven

---

## Configuration

Les principaux paramètres sont définis dans `application.yaml` :

```yaml
batch:
  address:
    input-file: file:./data/csv/adresses-79.csv
    chunk-size: 500
```

* `input-file` : fichier CSV BAN à importer
* `chunk-size` : taille des lots traités par Spring Batch

Le fichier CSV doit être placé dans :

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

## Exécution

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

## Base de données

La base SQLite est créée automatiquement dans :

```text
data/adresses.db
```

La table: ban_address_final
Correspond au résultat de l'import (cependant les conflits métiers ne seront pas importé)

La table: address_reject
Contient toutes les erreurs de dupplication ou de conflit métier. Les duplications ont été inséré mais pas les conflits métiers

La table: address_sync_plan
Contient les informations de votre dernier import (lignes ajoutées/modifiées/supprimées)

---

## Configuration avancée

L'application utilise SQLite. Certains paramètres peuvent être ajustés pour privilégier soit la performance, soit la sécurité des données.

Ces paramètres sont particulièrement importants lors de l'import de fichiers volumineux. Ils sont à modifier

#### `PRAGMA journal_mode`

Définit le mode de journalisation utilisé par SQLite.

| Valeur   | Performance |    Sécurité | Usage conseillé                        |
| -------- | ----------: | ----------: | -------------------------------------- |
| `DELETE` |     Moyenne |      Élevée | Mode classique SQLite                  |
| `WAL`    |      Élevée |      Élevée | Recommandé pour les imports volumineux |
| `OFF`    | Très élevée | Très faible | Déconseillé                            |

Valeur recommandée :

```sql
PRAGMA journal_mode = WAL;
```

Le mode `WAL` écrit les changements dans un fichier séparé avant de les intégrer à la base principale. Il est généralement plus performant pour les écritures importantes.

#### `PRAGMA synchronous`

Définit le niveau de synchronisation des écritures sur le disque.

| Valeur   | Performance | Sécurité | Usage conseillé           |
| -------- | ----------: | -------: | ------------------------- |
| `FULL`   |  Plus lente | Maximale | Environnement sensible    |
| `NORMAL` |       Bonne |    Bonne | Recommandé pour ce projet |
| `OFF`    | Très rapide |   Faible | Déconseillé               |

Valeur recommandée pour un bon compromis :

```sql
PRAGMA synchronous = NORMAL;
```

Avec `NORMAL`, SQLite effectue moins de synchronisations disque qu'avec `FULL`. Cela améliore les performances, avec un risque limité en cas d'arrêt brutal de la machine pendant l'import.

#### `PRAGMA cache_size`

Définit la quantité de mémoire utilisée par SQLite pour mettre en cache les pages de la base.

Une valeur négative indique une taille en kibioctets.

Exemples :

```sql
PRAGMA cache_size = -65536;   -- environ 64 Mo
PRAGMA cache_size = -262144;  -- environ 256 Mo
PRAGMA cache_size = -524288;  -- environ 512 Mo
```

| Valeur    | Performance | Mémoire utilisée |
| --------- | ----------: | ---------------: |
| `-65536`  |    Correcte |           Faible |
| `-262144` |       Bonne |          Moyenne |
| `-524288` |  Très bonne |           Élevée |

Valeur recommandée :

```sql
PRAGMA cache_size = -262144;
```

Augmenter cette valeur peut améliorer les performances sur les gros imports, mais augmente la consommation mémoire.

#### `PRAGMA temp_store`

Définit où SQLite stocke les structures temporaires utilisées pendant certains tris, jointures ou regroupements.

| Valeur   | Performance | Mémoire utilisée | Usage conseillé                        |
| -------- | ----------: | ---------------: | -------------------------------------- |
| `FILE`   |  Plus lente |           Faible | Machines avec peu de RAM               |
| `MEMORY` | Plus rapide |      Plus élevée | Recommandé pour les imports volumineux |

Valeur recommandée :

```sql
PRAGMA temp_store = MEMORY;
```

Cette option permet de limiter les accès disque pendant les traitements SQL complexes.

#### Configuration rapide

Configuration recommandée pour privilégier les performances pendant l'import :

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = NORMAL;
PRAGMA cache_size = -262144;
PRAGMA temp_store = MEMORY;
```

Cette configuration est adaptée à un import volumineux sur une machine stable, idéalement avec un SSD.

#### Configuration sécurisée

Configuration recommandée si la sécurité des écritures est prioritaire :

```sql
PRAGMA journal_mode = WAL;
PRAGMA synchronous = FULL;
PRAGMA cache_size = -65536;
PRAGMA temp_store = FILE;
```

Cette configuration est plus prudente, mais moins rapide.

---

### Exécution avancée

Pour les imports volumineux, il est possible d'augmenter la mémoire allouée à la JVM.

Exemple :

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xms2g -Xmx4g"
```

```bash
java "-Dspring-boot.run.jvmArguments=-Xms2g -Xmx4g" -jar .\target\tp-adresse-test-0.0.1-SNAPSHOT.jar
```

Paramètres :

| Paramètre | Description                                   |
| --------- | --------------------------------------------- |
| `-Xms2g`  | Mémoire initiale allouée à la JVM : 2 Go      |
| `-Xmx4g`  | Mémoire maximale autorisée pour la JVM : 4 Go |

Exemple avec plus de mémoire :

```bash
mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Xms4g -Xmx8g"
```

Recommandations :

| Taille du fichier | Mémoire JVM recommandée |
| ----------------- | ----------------------- |
| Petit fichier     | `-Xms512m -Xmx1g`       |
| Fichier moyen     | `-Xms1g -Xmx2g`         |
| Gros fichier      | `-Xms2g -Xmx4g`         |
| Très gros fichier | `-Xms4g -Xmx8g`         |

Augmenter la mémoire JVM peut améliorer la stabilité du traitement sur les gros volumes, mais ne remplace pas les optimisations SQLite.

Pour de meilleures performances, il est recommandé d'exécuter l'import sur un SSD.
