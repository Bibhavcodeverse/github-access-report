# GitHub Access Report Service

This is a Spring Boot application that connects to the GitHub API to generate a report showing which users have access to which repositories within a given organization.

## How to run the project

### Prerequisites
- Java 17 or higher
- Maven (or use the included wrapper `mvnw`)
- A GitHub Personal Access Token (PAT) with read access to the organization's repositories and collaborators.

### Running locally
1. Set your GitHub token as an environment variable:
   ```powershell
   # Windows PowerShell
   $env:GITHUB_TOKEN="your_token_here"
   ```
   ```bash
   # Linux/macOS
   export GITHUB_TOKEN="your_token_here"
   ```
2. Build and run the application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
The application will start on port `8080`.

## How authentication is configured

Standard GitHub authentication is performed by placing the PAT token in the HTTP `Authorization: Bearer <token>` header for all API requests. In this application, this is configured inside `application.yml` via the `${GITHUB_TOKEN}` environment variable. The `GithubClient` service automatically extracts this resource and builds a default `Authorization` header in the Spring WebFlux `WebClient`.

## How to call the API endpoint

You can call the API using a tool like `curl`, Postman, or your browser:

```bash
curl "http://localhost:8080/api/v1/github/access-report?org=your-org-name"
```

The API responds with a JSON object mapping each user to the list of repositories they have access to and their roles (`admin`, `write`, `read`, `none`).

### Example Response
```json
{
  "octocat": [
    {
      "repository": "hello-world",
      "role": "admin"
    },
    {
      "repository": "test-repo",
      "role": "write"
    }
  ]
}
```

## Notes on implementation

- Uses GitHub PAT via `Authorization: Bearer <token>`
- Handles GitHub pagination by reading the `Link` header and following `rel="next"`
- Uses `per_page=100` to reduce call count
- Uses Spring WebFlux (`WebClient`) and `flatMap` concurrency limiting to fetch collaborators for repositories in parallel
  - This avoids fully sequential repo-by-repo collaborator calls
  - Efficiently scales network I/O without blocking threads and avoids hitting secondary rate limits abruptly
- Includes basic error handling for:
  - invalid input (`org` missing)
  - missing/invalid token
  - GitHub not found/forbidden cases (e.g. 403/404 on specific repositories)
  - network failures

## Assumptions made

- PAT has enough permissions to list organization repos and repository collaborators (e.g., `read:org`, `repo`).
- Private repos/collaborators are only returned if the token has the proper scopes.
- Current version fetches all repos in the org and then all collaborators per repo.
- The same collaborator can appear in many repositories; the response groups and maps repository access roles per user.

## Scaling and optimization approach

Current project handles scaling in a practical way:
1. Pagination support for large organizations/repositories.
2. `per_page=100` to reduce total requests.
3. Parallel collaborator fetch calls across repositories using reactive streams to reduce total wall-clock time.

If org size grows significantly, next improvements would typically be:
1. Add short-term caching for report responses to avoid re-fetching identical data frequently.
2. Add rate-limit aware retry/backoff mechanisms (e.g. parsing `X-RateLimit-Reset`).
3. Persist snapshots in a database for historical reporting.
