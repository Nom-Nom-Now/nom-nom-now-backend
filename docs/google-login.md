# Google Login

## Übersicht

```
┌──────────┐     ①      ┌──────────┐     ②      ┌──────────┐
│ Frontend │  ────────►  │ Backend  │  ────────►  │  Google  │
│ :5173    │             │ :8080    │             │  OAuth2  │
└──────────┘             └──────────┘             └──────────┘
     ▲          ⑥             ▲          ③             │
     └──── redirect ──────────┘◄──── callback ─────────┘
                          ④ User anlegen / finden
                          ⑤ Session erstellen
```

1. Frontend leitet zu `{BACKEND_URL}/oauth2/authorization/google`
2. Spring Security redirected zu Google
3. User loggt sich ein → Google callback an `/login/oauth2/code/google`
4. Backend erstellt oder findet den `AppUser`
5. Session wird angelegt
6. Redirect zum Frontend → Session-Cookie wird bei weiteren Requests mitgeschickt

---

## Google Cloud Console einrichten

Bevor irgendetwas funktioniert, brauchst du OAuth2 Credentials von Google:

### 1 — Projekt erstellen

1. Öffne die [Google Cloud Console](https://console.cloud.google.com/)
2. Erstelle ein neues Projekt (oder wähle ein bestehendes)

### 2 — OAuth-Zustimmungsbildschirm konfigurieren

1. **APIs & Dienste → OAuth-Zustimmungsbildschirm**
2. User Type: **Extern**
3. App-Name, Support-E-Mail etc. ausfüllen
4. Scopes hinzufügen: `email`, `profile`, `openid`
5. Unter **Testnutzer** deine eigene Google-Mail hinzufügen (solange die App nicht verifiziert ist, können nur Testnutzer sich einloggen)

### 3 — OAuth2 Client ID erstellen

1. **APIs & Dienste → Anmeldedaten → Anmeldedaten erstellen → OAuth-Client-ID**
2. Anwendungstyp: **Webanwendung**
3. **Autorisierte Redirect-URIs** eintragen:

| Umgebung | Redirect URI |
|----------|-------------|
| Lokal | `http://localhost:8080/login/oauth2/code/google` |
| Produktion | `https://api.deinedomain.com/login/oauth2/code/google` |

4. Client-ID und Client-Secret kopieren

> ⚠️ Die Redirect-URI muss **exakt** übereinstimmen – inkl. Protokoll, Port und Pfad.

---

## Lokale Einrichtung

### 1 — Env-Variablen in `.env` setzen

```bash
GOOGLE_CLIENT_ID=123456789-abc.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-...
FRONTEND_URL=http://localhost:5173
```

### 2 — Backend starten

```bash
export $(xargs < .env)
./mvnw spring-boot:run
```

### 3 — Testen

Öffne im Browser:

```
http://localhost:8080/oauth2/authorization/google
```

Du wirst zu Google weitergeleitet. Nach dem Login landest du auf `http://localhost:5173` und hast eine aktive Session.

Session prüfen:

```bash
# Nach Login im Browser den Cookie kopieren, dann:
curl -b "JSESSIONID=<dein-cookie>" http://localhost:8080/auth/me
```

---

## Deployment / Produktion

### GitHub Secrets anlegen

Die CD-Pipeline generiert die `.env` auf dem Server aus GitHub Secrets.  
Unter **Repository → Settings → Environments → production → Secrets** diese Secrets anlegen:

| Secret | Beispiel |
|--------|----------|
| `POSTGRES_PASSWORD` | `sicheres-passwort` |
| `APP_DB_USERNAME` | `nnn_app` |
| `APP_DB_PASSWORD` | `sicheres-passwort` |
| `GOOGLE_CLIENT_ID` | `123...apps.googleusercontent.com` |
| `GOOGLE_CLIENT_SECRET` | `GOCSPX-...` |
| `FRONTEND_URL` | `https://app.deinedomain.com` |

> Die bestehenden Secrets (`SSH_HOST`, `SSH_USER`, `SSH_PRIVATE_KEY`, `SSH_PORT`, `APP_DIR`) bleiben unverändert.

### Google Cloud Console anpassen

In den OAuth2 Credentials eine **zusätzliche** Redirect-URI hinzufügen:

```
https://api.deinedomain.com/login/oauth2/code/google
```

### Checkliste

- [ ] GitHub Secrets angelegt (siehe Tabelle oben)
- [ ] Redirect-URI in der Google Cloud Console eingetragen
- [ ] App im OAuth-Zustimmungsbildschirm verifiziert (für öffentlichen Zugang)
- [ ] HTTPS aktiv (OAuth2 erfordert HTTPS in Produktion)

---

## Env-Variablen Referenz

| Variable | Beschreibung | Default |
|----------|-------------|---------|
| `GOOGLE_CLIENT_ID` | OAuth2 Client ID aus der Google Cloud Console | – (Pflicht) |
| `GOOGLE_CLIENT_SECRET` | OAuth2 Client Secret | – (Pflicht) |
| `FRONTEND_URL` | Frontend-URL für CORS und Post-Login-Redirect | `http://localhost:5173` |

---

## Autorisierung

| Methode | Pfad | Zugriff |
|---------|------|---------|
| `GET` | `/recipes/**`, `/categories/**` | 🌍 Öffentlich |
| `POST` | `/recipes`, `/categories` | 🔒 Authentifiziert |
| `GET` | `/auth/me` | 🔒 Authentifiziert |

---

## Frontend-Integration

```js
const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

// Login starten – leitet den Browser weiter
function login() {
  window.location.href = `${BACKEND_URL}/oauth2/authorization/google`;
}

// API-Calls – Session-Cookie wird automatisch mitgeschickt
async function getRecipes() {
  const res = await fetch(`${BACKEND_URL}/recipes`, {
    credentials: 'include',
  });
  return res.json();
}

// Eingeloggten User abfragen
async function getCurrentUser() {
  const res = await fetch(`${BACKEND_URL}/auth/me`, {
    credentials: 'include',
  });
  if (!res.ok) return null; // nicht eingeloggt
  return res.json();
}
```
