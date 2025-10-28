# nom-nom-now-backend
## Local startup guide

### Prerequisites
- JDK 21 (or a compatible distribution such as Temurin or Azul)
- Docker Desktop or Docker Engine with Compose V2
- Optional: `psql` for inspecting the database manually

### Configure environment

1. Create a `.env` file in the repository root; it is shared by Docker Compose and the Spring Boot app.
2. Add the following variables (update the passwords to something secure for your machine):

   ```bash
   POSTGRES_PASSWORD=changeme
   APP_DB_USERNAME=nnn_app
   APP_DB_PASSWORD=changeme
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/nnn
   SPRING_DATASOURCE_USERNAME=${APP_DB_USERNAME}
   SPRING_DATASOURCE_PASSWORD=${APP_DB_PASSWORD}
   SPRING_DOCKER_COMPOSE_ENABLED=false
   ```

### Start infrastructure
1. Start PostgreSQL: `docker compose up -d postgres`
2. Apply migrations (runs once per schema change): `docker compose --profile migrate run --rm flyway`

The database persists in the `pgdata` Docker volume, so recreating containers does not remove data.

### Run the backend
1. Launch the application:
    ```bash 
    export $(xargs < .env)
    ./mvnw spring-boot:run 
    ```
2. The API becomes available at http://localhost:8080 after the Spring banner appears.

### Shut down
- Stop the app with `Ctrl+C`.
- Tear down containers when finished: `docker compose down`.


## Current state

## Conventions

To keep the repository clean and consistent, we follow clear conventions for branches, commits, and structure.

### Branch Naming
Each branch name starts with a type prefix, followed by the Jira issue ID and a short, descriptive name in kebab-case.

**Format:**
`<type>/<issue-id>-<short-description>`

**Examples:**
`feat/NNN-42-add-login-page`  
`fix/NNN-17-resolve-null-pointer`  
`docs/NNN-36-update-readme`

| Prefix | Purpose |
|--------|----------|
| feat/ | New feature |
| fix/ | Bug fix |
| docs/ | Documentation |
| chore/ | Maintenance or cleanup |
| refactor/ | Code restructure |
| test/ | Tests |
| ci/ | CI/CD changes |

### Commit Messages
Commit messages describe what and why a change was made, in a consistent format.

**Format:**
`<type>(<scope>): <short summary>`

**Example:**
`feat(auth): add token refresh endpoint`

Rules:
- Use imperative tone (“add”, “fix”, “update”)  
- Keep under 80 characters  
- Reference Jira issue ID when applicable  

### Repository Structure
A clear layout helps everyone find things quickly.

```text
src/        # Application code
docs/       # Documentation
config/     # Configuration files
scripts/    # Helper or deployment scripts
