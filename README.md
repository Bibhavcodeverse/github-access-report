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

## Assumptions and Design Decisions

1. **Reactive Programming context:** Used **Spring WebFlux (`WebClient`)** to query the GitHub API asynchronously. Since fetching organization repositories and their individual collaborators can involve many network operations (100+ repos = 100+ API requests), traditional blocking sequential calls would severely degrade performance. WebFlux handles it concurrently.
2. **Concurrency Limiting:** The code limits flatMap concurrency to `10` simultaneous repository collaborator fetches to ensure we do not hit GitHub's secondary rate limits abruptly.
3. **Pagination:** GitHub API automatically paginates results (default 30, max 100). The service sets `per_page=100` and recursively traverses the `rel="next"` Link header until all pages of repositories and collaborators are retrieved.
4. **Error Handling:** If the underlying token does not have access to view a specific repository's collaborators (e.g. returns 404), the `WebClient` swallows the error for that single repository and gracefully continues.
