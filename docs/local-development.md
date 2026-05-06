# Local Development

This is the copy-paste path for developing Nom Nom Now locally without Google OAuth.

## What You Get

- Backend on `http://localhost:8080`
- Frontend on `http://localhost:5173`
- PostgreSQL in Docker
- Flyway migrations applied locally
- Automatic backend auth with one test account
- No Google Cloud setup required

## Requirements

| Tool | Version |
|------|---------|
| Java | 25 or newer |
| Docker | Docker Compose V2 |
| Node.js | 20 LTS or newer |
| npm | Comes with Node.js |

Check them:

```bash
java -version
docker version
docker compose version
node --version
npm --version
```

## Backend Setup

From `nom-nom-now-backend`, create `.env`:

```dotenv
POSTGRES_PASSWORD=changeme
APP_DB_USERNAME=nnn_app
APP_DB_PASSWORD=changeme

SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SPRING_DOCKER_COMPOSE_ENABLED=false
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nnn
SPRING_DATASOURCE_USERNAME=nnn_app
SPRING_DATASOURCE_PASSWORD=changeme

FRONTEND_URL=http://localhost:5173
APP_DEV_USER_EMAIL=dev@nomnomnow.local
APP_DEV_USER_NAME="Local Dev User"
```

Start the database and apply migrations:

```bash
docker compose up -d postgres
docker compose --profile migrate run --rm flyway
```

Start the backend:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

Windows PowerShell:

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

Verify the backend:

```bash
curl http://localhost:8080/auth/me
curl http://localhost:8080/categories
```

Expected `/auth/me` response:

```json
{
  "id": 1,
  "email": "dev@nomnomnow.local",
  "name": "Local Dev User"
}
```

The `id` can be different. The email and name should match your `.env`.

## Frontend Setup

From `nom-nom-now-frontend`, install dependencies:

```bash
npm ci
```

Use this in `.env.development`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_BACKEND_URL=http://localhost:8080
```

Start the frontend:

```bash
npm run dev
```

Open:

```text
http://localhost:5173
```

In `dev` backend mode the login button redirects through the backend and lands on `/home` without Google.

## How Dev Auth Works

When `SPRING_PROFILES_ACTIVE=dev` is active:

- Google OAuth is disabled.
- Every request gets an authenticated user automatically.
- The user is stored in `app.app_user`.
- The user key is `google_id = dev:<APP_DEV_USER_EMAIL>`.
- `POST /recipes` works without copying browser cookies or logging into Google.

This mode is only for local development. Do not use it in production.

## Create A Recipe From Curl

JSON without image:

```bash
curl -X POST http://localhost:8080/recipes \
  -H 'Content-Type: application/json' \
  --data '{
    "name": "Dev Test Recipe",
    "instructions": "Mix everything.",
    "cookingTime": 15,
    "categoryIds": [1],
    "components": [
      { "name": "Flour", "quantity": 100, "unit": "GRAM" }
    ]
  }'
```

Multipart with image:

```bash
curl -X POST http://localhost:8080/recipes \
  -F 'recipe={
    "name": "Dev Test Recipe With Image",
    "instructions": "Mix everything.",
    "cookingTime": 15,
    "categoryIds": [1],
    "components": [
      { "name": "Flour", "quantity": 100, "unit": "GRAM" }
    ]
  };type=application/json' \
  -F 'image=@/absolute/path/to/image.jpg;type=image/jpeg'
```

## Reset Local Data

Stop containers:

```bash
docker compose down
```

Delete the local database volume:

```bash
docker compose down -v
```

Then start again:

```bash
docker compose up -d postgres
docker compose --profile migrate run --rm flyway
```

## Common Problems

### `column ... does not exist`

Run migrations:

```bash
docker compose --profile migrate run --rm flyway
```

If the schema is badly out of sync during local development, reset the volume with `docker compose down -v`.

### `POST /recipes` redirects to Google

Your backend is not running with the dev profile. Check:

```bash
echo $SPRING_PROFILES_ACTIVE
```

It must be:

```text
dev
```

### Frontend calls the wrong backend URL

Check `nom-nom-now-frontend/.env.development`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
VITE_BACKEND_URL=http://localhost:8080
```

Restart `npm run dev` after changing Vite env files.

### Port already in use

Find the process:

```bash
lsof -i :8080
lsof -i :5173
```

Stop that process or use a different port.
