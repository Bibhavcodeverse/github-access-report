# GitHub Access Report System – Detailed Assignment Submission

---

## 1. Introduction
**Repository Link:** [https://github.com/Bibhavcodeverse/github-access-report](https://github.com/Bibhavcodeverse/github-access-report)

The GitHub Access Report System is a backend solution built using Spring Boot that connects with the GitHub REST API to generate a detailed access report for an organization.

In growing organizations, it becomes challenging to keep track of which users have access to specific repositories, especially as the number of repositories and contributors increases. This system solves that problem by presenting the access data in a clear, structured, and user-focused manner.

The application collects information about repositories and their collaborators from GitHub and processes it into a well-organized report. This report improves transparency, supports security audits, and makes access management more efficient.

---

## 2. Objective
The primary objective of this project is to:
- Provide a centralized view of repository access within a GitHub organization
- Map users to the repositories they can access
- Build a scalable backend system capable of handling large datasets
- Expose the data through a REST API in a structured JSON format

---

## 3. Approach (Step-by-Step Explanation)
The system follows a systematic workflow:

### Step 1: Authentication
The application authenticates with GitHub using a Personal Access Token (PAT). This token is required to securely access organization-level data, including private repositories and collaborators.

### Step 2: Fetch Repositories
Using the GitHub API:
`GET /orgs/{org}/repos`
the system retrieves all repositories belonging to the specified organization.

### Step 3: Fetch Collaborators
For each repository, the application fetches the list of users who have access:
`GET /repos/{org}/{repo}/collaborators`

### Step 4: Data Transformation
Initially, data is retrieved in the form:
- Repository → List of Users

This is transformed into:
- User → List of Repositories

This transformation improves readability and aligns with reporting needs.

### Step 5: API Response
Finally, the transformed data is returned as a JSON response through a REST endpoint.

---

## 4. System Design
The application is designed using a layered architecture to ensure separation of concerns and maintainability.

### Controller Layer
Handles incoming HTTP requests and exposes REST endpoints to the client.

### Service Layer
Contains the core business logic, including:
- Fetching data
- Processing and transforming results
- Managing concurrency

### Client Layer
Responsible for interacting with GitHub APIs using Spring WebFlux (`WebClient`). It handles:
- API requests
- Headers and authentication
- Pagination

### Model Layer
Defines data structures such as:
- Repository model
- Collaborator model
- RepositoryAccess model

### Configuration Layer
Manages:
- GitHub token configuration
- WebClient setup

---

## 5. API Details

**Endpoint:**
`GET /api/v1/github/access-report?org={organization}`

**Description:**
Returns a mapping of users and the repositories they have access to within the organization.

**Sample Response:**
```json
{
  "user1": [
    {"repository": "repo1", "role": "admin"},
    {"repository": "repo2", "role": "write"}
  ],
  "user2": [
    {"repository": "repo2", "role": "read"},
    {"repository": "repo3", "role": "write"}
  ]
}
```

**Response Explanation:**
- Key → GitHub username
- Value → List of repositories accessible by that user, paired with their specific role.

---

## 6. Authentication
Authentication is handled using a GitHub Personal Access Token (PAT).

**Key Points:**
- Stored securely in `application.yml` via environment variables.
- Sent in request headers: `Authorization: Bearer <token>`

**Required permissions:**
- `repo` (for private repositories)
- `read:org`

This ensures secure and authorized access to GitHub resources.

---

## 7. Scalability Considerations
The system is designed to handle large-scale organizations efficiently.

**Challenges:**
- Large number of repositories (100+)
- Large number of users (1000+)
- Multiple API calls required synchronously would block threads

**Solutions:**

**Parallel Processing:**
Instead of making sequential API calls, the system uses reactive streams (`Flux.flatMap`) to execute multiple repository collaborator fetch requests in parallel, significantly reducing overall response time. Concurrency is strategically limited (e.g., 10 concurrent requests) to prevent secondary rate limits.

**Reactive Programming:**
Spring `WebClient` is used for non-blocking API calls, allowing excellent resource utilization and improved performance during heavy network I/O without dedicating a specific thread per network request.

**Pagination Handling:**
GitHub APIs return data in pages (max 100 per page). The system iteratively fetches all pages by dynamically following the `rel="next"` link header to ensure robust and complete data retrieval, avoiding hard-coded offset loops.

---

## 8. Rate Limiting Strategy
To aggressively respect GitHub's API rate limits (primary limit of 5,000 requests/hour and secondary limits on burst/abuse):
- **Concurrency Caps:** `flatMap` concurrency is strictly limited to prevent bursting too many simultaneous calls.
- **Max Page Size:** `per_page=100` minimizes the total number of HTTP requests made.
- **Fail-Safe Processing:** If a specific repository requires more calls than the limit allows or returns a `403 Forbidden`, the reactive pipeline can skip the error gracefully using `onErrorResume`.

---

## 9. Assumptions Made
- The configured PAT has enough permissions (`read:org`, `repo`) to list organization repositories and view repository collaborators.
- Private repositories and their respective collaborators are only processed if the token has the necessary scopes.
- The same collaborator can appear across multiple repositories. The response effectively maps roles on a per-repository basis to the user.
- The organization name provided in the REST API request is a valid GitHub entity.

---

## 10. Design Decisions
- **WebFlux over Standard Threading:** A fully reactive approach (`Spring WebFlux`) was favored over standard asynchronous models (`CompletableFuture` + Executors) because it intrinsically handles I/O bound operations more efficiently without exhausting threading pools under extreme loads.
- **Forward-Only Link Pagination:** Since GitHub robustly handles pagination via standard Link headers, we rely on extracting and navigating these URLs instead of constructing fixed manual parameters, ensuring resilience against future changes in GitHub's REST specification.

---

## 11. Future Enhancements
- **Caching Implementation:** Integrate Caffeine Cache to store access reports temporarily. Repeated calls for the same organization wouldn't hit the GitHub API again, speeding up responses immensely and preserving valuable rate limit points.
- **Global Exception Handling:** Introduce `@ControllerAdvice` to seamlessly catch specific errors (e.g., `401 Unauthorized`, `403 Forbidden`, `404 Not Found`) and map them to clean JSON standard error responses instead of generic server faults.
- **Incremental Updates:** For massive enterprise organizations, implement Webhook-based integration to mutate our local cache instantly when access changes on GitHub's side, rather than fetching everything from scratch on-demand.
- **Database Persistence:** Persist access snapshots continuously in a database (like PostgreSQL or MongoDB) enabling historical reporting and point-in-time audit trails.
