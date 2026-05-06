# 🍜 nom-nom-now-backend

Spring Boot REST API für Nom Nom Now – Rezepte erstellen, teilen und entdecken.

> **Tech-Stack:** Java 25 · Spring Boot 4 · PostgreSQL · Flyway · Docker Compose · OAuth2 (Google) · lokales Dev-Profil ohne Google

---

## Schnellstart

Für normale lokale Entwicklung nutze das `dev`-Profil. Es deaktiviert Google OAuth2 und meldet jede Anfrage automatisch als lokalen Testnutzer an.

### Voraussetzungen

| Tool | Version |
|------|---------|
| JDK | 25+ (Temurin, Azul o.ä.) |
| Docker | mit Compose V2 |

### 1 — `.env` anlegen

```bash
# Datenbank
POSTGRES_PASSWORD=changeme
APP_DB_USERNAME=nnn_app
APP_DB_PASSWORD=changeme

# Spring Boot
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SPRING_DOCKER_COMPOSE_ENABLED=false
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nnn
SPRING_DATASOURCE_USERNAME=nnn_app
SPRING_DATASOURCE_PASSWORD=changeme

# Frontend + lokaler Testnutzer
FRONTEND_URL=http://localhost:5173
APP_DEV_USER_EMAIL=dev@nomnomnow.local
APP_DEV_USER_NAME="Local Dev User"
```

### 2 — Infrastruktur starten

```bash
docker compose up -d postgres
docker compose --profile migrate run --rm flyway
```

### 3 — Backend starten

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

Für Windows PowerShell:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim().Trim('"')
        Set-Item "Env:$name" $value
    }
}
./mvnw spring-boot:run
```

API erreichbar unter `http://localhost:8080`.

### 4 — Prüfen, ob alles läuft

```bash
curl http://localhost:8080/auth/me
curl http://localhost:8080/categories
```

`/auth/me` muss den lokalen Testnutzer zurückgeben. Wenn das funktioniert, können geschützte Endpunkte wie `POST /recipes` ohne Google Login entwickelt werden.

### 5 — Herunterfahren

```bash
# App: Ctrl+C
docker compose down          # Container stoppen
# docker compose down -v     # + Datenbank-Volume löschen
```

---

## Authentifizierung

Lokale Entwicklung:

- `SPRING_PROFILES_ACTIVE=dev`
- kein Google Login nötig
- Backend erstellt/benutzt automatisch den Testnutzer aus `APP_DEV_USER_EMAIL` und `APP_DEV_USER_NAME`
- Der Frontend-Login-Button darf weiterhin geklickt werden; im Dev-Profil redirected `/oauth2/authorization/google` direkt zurück zu `${FRONTEND_URL}/home`

Produktion oder echtes Login-Testen:

- Google OAuth2 Login – Details siehe [`docs/google-login.md`](docs/google-login.md)

Komplette lokale Schritt-für-Schritt-Anleitung: [`docs/local-development.md`](docs/local-development.md).

| Methode | Pfad | Zugriff |
|---------|------|---------|
| `GET` | `/recipes` · [Pagination](docs/paging.md) | 🌍 Öffentlich |
| `GET` | `/recipes/**`, `/categories/**` | 🌍 Öffentlich |
| `POST` | `/recipes`, `/categories` | 🔒 Authentifiziert |
| `GET` | `/auth/me` | 🔒 Authentifiziert |

---

## Flyway-Migrationen

Skripte liegen in `flyway/sql/` und folgen dem Schema:

```
V<nummer>__<beschreibung>.sql     z.B.  V5__add_ratings.sql
```

**Regeln:**
- Fortlaufende Nummern, `snake_case` Beschreibung
- Bereits ausgeführte Migrationen **niemals** ändern oder löschen
- `${appUserPassword}` kann als Placeholder genutzt werden (siehe `V1`)

Migration anwenden:

```bash
docker compose --profile migrate run --rm flyway
```

---

## Projektstruktur

```
├── compose.yaml                   # Docker Compose (Postgres, Flyway, Backend)
├── Dockerfile
├── flyway/sql/                    # Datenbank-Migrationen
├── docs/                          # Dokumentation
└── src/main/java/.../nnnbackend/
    ├── config/                    # Security, CORS
    ├── controller/                # REST-Endpunkte
    ├── dto/                       # Request/Response DTOs
    │   ├── request/
    │   └── response/
    ├── entity/                    # JPA-Entities
    ├── exception/                 # Fehlerbehandlung
    ├── mapper/                    # Entity ↔ DTO
    ├── repository/                # Spring Data Repositories
    ├── service/                   # Business-Logik
    └── user/                      # Auth, AppUser, CurrentUser
```

---

## Konventionen

### Branches

`<type>/<issue-id>-<beschreibung>` → z.B. `feat/NNN-42-add-login-page`

| Prefix | Zweck |
|--------|-------|
| `feat/` | Neues Feature |
| `fix/` | Bugfix |
| `docs/` | Dokumentation |
| `chore/` | Aufräumarbeiten |
| `refactor/` | Umbau |
| `test/` | Tests |
| `ci/` | CI/CD |

### Commits

```
<type>(<scope>): <zusammenfassung>
```

Imperativ, max. 80 Zeichen — z.B. `feat(auth): add token refresh endpoint`
