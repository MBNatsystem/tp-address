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
    input-file: classpath:adresses-79.csv
    chunk-size: 500
```

* `input-file` : fichier CSV BAN à importer
* `chunk-size` : taille des lots traités par Spring Batch

Le fichier CSV doit être placé dans :

```text
src/main/resources/
```

Exemple :

```text
src/main/resources/adresses-79.csv
```

---

## Compilation

```bash
./mvnw clean package
```

---

## Exécution

### Import complet

```bash
./mvnw spring-boot:run
```

### Import par code postal

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=codePostal=79240"
```

### Import par code INSEE

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=codeInsee=79002"
```

### Import par code postal et code INSEE

```bash
./mvnw spring-boot:run "-Dspring-boot.run.arguments=codePostal=79240 codeInsee=79002"
```

---

## Base de données

La base SQLite est créée automatiquement dans :

```text
data/adresses.db
```