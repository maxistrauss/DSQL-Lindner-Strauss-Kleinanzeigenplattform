# Kleinanzeigen Web-Anwendung

Eine moderne Web-App zum Aufgeben und Durchsuchen von Kleinanzeigen, entwickelt mit **Spring Boot** und **Gradle**.

## Features

- Anzeigen aufgeben und kommentieren
- Kategorien, Suche und Pagination
- Bilder-Upload via GridFS
- Benutzerfreundliches Web-Frontend mit Thymeleaf
- Datenhaltung in MongoDB (keine Benutzer-Registrierung nötig)

## Technologien

- Java 21
- Spring Boot (Web, Data MongoDB, Thymeleaf)
- Gradle (Build, Dependency Management)
- MongoDB
- GitLab (Sourcecode und Collaboration)

## Installation & Setup

### Systemanforderungen

- Java 21
- MongoDB (lokal oder auf Server)
- Gradle
- Git

### Projektstruktur

- `/src/main/java` – Java-Code (Controller, Models etc.)
- `/src/main/resources/templates` – Thymeleaf HTML-Templates
- `/src/main/resources/application.properties` – Konfiguration

### Profile: Entwicklung & Produktion

Die Anwendung unterstützt **Spring Boot Profiles** für unterschiedliche Umgebungen:

- **Entwicklung (dev):** ohne Datenbank-Authentifizierung
- **Produktion (prod):** mit Benutzer/Passwort für MongoDB

**Profil setzen & Anwendung starten:**

```bash
# Produktion:
export SPRING_PROFILES_ACTIVE=prod
java -jar your-application.jar

# Entwicklung:
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

Profile sorgen für sichere und flexible Konfiguration – alle Einstellungen liegen in `application-dev.properties` und
`application-prod.properties`.

## Screenshot

*Füge hier einen Screenshot deiner Anwendung im Browser ein.*

***
