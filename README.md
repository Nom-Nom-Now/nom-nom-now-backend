# nom-nom-now-backend
## Local startup guide

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

