# Kleinanzeigen

Kleinanzeigen ist eine umfassende Web-Anwendung, die es Benutzern ermöglicht, auf einfache und intuitive Weise
Kleinanzeigen zu erstellen, zu verwalten und zu durchsuchen. Die Plattform ist darauf ausgelegt, eine
benutzerfreundliche Erfahrung für das Kaufen und Verkaufen von Artikeln zu bieten.

## Features

* **Anzeigenverwaltung:** Benutzer können Anzeigen mit Titel, Beschreibung, Preis und Bildern erstellen. Bestehende
  Anzeigen können jederzeit bearbeitet oder gelöscht werden.
* **Kategorisierung:** Um die Übersichtlichkeit zu wahren, werden alle Anzeigen in vordefinierten Kategorien eingeteilt.
  Dies erleichtert das gezielte Suchen und Finden von Artikeln.
* **Bild-Upload:** Ein Bild sagt mehr als tausend Worte. Benutzer können Bilder zu ihren Anzeigen hochladen, um
  potenziellen Käufern einen besseren Eindruck vom Artikel zu vermitteln.
* **Kommentarfunktion:** Benutzer können zu jeder Anzeige Kommentare hinterlassen, um Fragen zu stellen oder
  Verhandlungen zu führen.
* **Leistungsstarke Suche:** Eine integrierte Suchfunktion ermöglicht es Benutzern, das gesamte Anzeigen-Portfolio
  schnell und effizient nach bestimmten Artikeln oder Stichwörtern zu durchsuchen.

## Verwendete Technologien

* **Backend:**
    * **Java 21:** Die Anwendung ist in der neuesten Version von Java geschrieben.
    * **Spring Boot 3.5.7:** Das robuste Framework für die schnelle Entwicklung von Web-Anwendungen.
    * **Spring Data MongoDB:** Für die nahtlose Integration und Interaktion mit der MongoDB-Datenbank.
* **Frontend:**
    * **Thymeleaf:** Eine moderne serverseitige Java-Template-Engine für die Erstellung dynamischer Webseiten.
* **Build-Tool:**
    * **Gradle:** Ein leistungsstarkes Build-Automatisierungstool.

## Projektstruktur

Das Projekt ist logisch und übersichtlich strukturiert, um die Wartbarkeit und Weiterentwicklung zu erleichtern:

* `src/main/java`: Enthält den gesamten Java-Quellcode der Anwendung.
    * `config`: Konfigurationsklassen, z.B. für die Datenbankinitialisierung.
    * `controller`: Spring MVC-Controller, die die Anfragen aus dem Web entgegennehmen und verarbeiten.
    * `model`: Die Datenmodelle (Entitäten), die die Struktur der in der Datenbank gespeicherten Daten definieren.
    * `repository`: Spring Data-Repositories für den Zugriff auf die Datenbank.
    * `service`: Service-Klassen, die die eigentliche Geschäftslogik der Anwendung enthalten.
* `src/main/resources`: Enthält alle statischen Ressourcen und Konfigurationsdateien.
    * `templates`: Die Thymeleaf-Vorlagen für das Frontend.
* `src/test/java`: Beherbergt die Unit- und Integrationstests.

## Systemanforderungen

* **Java 21** oder eine neuere Version.
* **MongoDB 4.4** oder eine neuere Version.

## Installation der Anwendung

1. **Klonen Sie das Repository:**
   ```bash
   git clone https://your-repository-url.git
   ```
2. **Navigieren Sie in das Projektverzeichnis:**
   ```bash
   cd Kleinanzeigen
   ```
3. **Erstellen Sie das Projekt mit Gradle:**
   ```bash
   ./gradlew build
   ```
4. **Führen Sie die Anwendung aus:**
   ```bash
   ./gradlew bootRun
   ```
   Die Anwendung ist nun unter `http://localhost:80` erreichbar.

## Datenbank-Einrichtung (MongoDB)

Die Anwendung erfordert eine laufende MongoDB-Instanz. Hier ist eine detaillierte Anleitung zur Installation und
Konfiguration von MongoDB auf einem Debian-basierten System (z.B. Ubuntu).

### 1. MongoDB Installieren

a. **Importieren Sie den öffentlichen GPG-Schlüssel von MongoDB:**

   ```bash
   sudo apt-get install gnupg curl
   curl -fsSL https://pgp.mongodb.com/server-6.0.asc | \
      sudo gpg -o /usr/share/keyrings/mongodb-server-6.0.gpg \
      --dearmor
   ```

b. **Erstellen Sie eine Listen-Datei für MongoDB:**

   ```bash
   echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-6.0.gpg ] https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/6.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-6.0.list
   ```

c. **Laden Sie die Paketdatenbank neu:**

   ```bash
   sudo apt-get update
   ```

d. **Installieren Sie die MongoDB-Pakete:**

   ```bash
   sudo apt-get install -y mongodb-org
   ```

e. **Starten Sie den MongoDB-Dienst:**

   ```bash
   sudo systemctl start mongod
   ```

f. **Überprüfen Sie den Status des Dienstes:**

   ```bash
   sudo systemctl status mongod
   ```

### 2. MongoDB Konfigurieren

a. **Verbinden Sie sich mit der MongoDB-Shell:**

   ```bash
   mongo
   ```

b. **Erstellen Sie einen administrativen Benutzer:**
Führen Sie die folgenden Befehle in der `mongo`-Shell aus, um einen Benutzer mit administrativen Rechten zu erstellen.
Ersetzen Sie `webService` und `!secure_PWD!` durch Ihre gewünschten Anmeldeinformationen.

   ```javascript
   use
admin
db.createUser(
    {
        user: "webService",
        pwd: "!secure_PWD!",
        roles: [{role: "root", db: "admin"}]
    }
)
   ```

c. **Aktivieren Sie die Authentifizierung:**
Öffnen Sie die MongoDB-Konfigurationsdatei in einem Texteditor:

   ```bash
   sudo nano /etc/mongod.conf
   ```

Fügen Sie die folgende `security`-Sektion hinzu oder kommentieren Sie sie aus, um die Authentifizierung zu aktivieren:

   ```yaml
   security:
     authorization: enabled
   ```

d. **Starten Sie den MongoDB-Dienst neu:**

   ```bash
   sudo systemctl restart mongod
   ```

### 3. Anwendungskonfiguration

Stellen Sie sicher, dass die `application.properties`-Datei in `src/main/resources/` die korrekten Anmeldeinformationen
für die Datenbank enthält:

```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.authentication-database=admin
spring.data.mongodb.database=kleinanzeigen_db
spring.data.mongodb.username=webService
spring.data.mongodb.password=!secure_PWD!
```

## Beispieldaten importieren

Um die Anwendung mit Beispieldaten zu füllen, können Sie die bereitgestellte `beispieldaten.json`-Datei verwenden. Diese
Datei enthält einige Beispielanzeigen, die in die Datenbank importiert werden können.

Führen Sie den folgenden Befehl im Hauptverzeichnis des Projekts aus, um die Daten zu importieren:

```bash
mongoimport --username webService --password !secure_PWD! --authenticationDatabase admin --db kleinanzeigen --collection ad --file beispieldaten.json --jsonArray
```

**Erklärung der Befehlsoptionen:**

* `--username webService`: Der Benutzername für die MongoDB-Authentifizierung.
* `--password !secure_PWD!`: Das Passwort für die MongoDB-Authentifizierung.
* `--authenticationDatabase admin`: Die Datenbank, in der der Benutzer authentifiziert wird.
* `--db kleinanzeigen`: Die Zieldatenbank, in die die Daten importiert werden sollen.
* `--collection ad`: Die Ziel-Collection innerhalb der Datenbank.
* `--file beispieldaten.json`: Der Pfad zur JSON-Datei mit den Beispieldaten.
* `--jsonArray`: Gibt an, dass die JSON-Datei ein Array von Dokumenten enthält.

## Screenshot

![Screenshot der Anwendung](placeholder.png)
