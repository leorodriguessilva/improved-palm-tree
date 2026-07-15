# Repository Ranking Service

A Spring Boot 3.x REST API service for ranking GitHub repositories using a deterministic scoring algorithm based on stars, forks, and recency.

## Quick Start

### Prerequisites
- Java 21+
- No local Maven installation is required for normal builds; use the included Maven wrapper.

### Build

```bash
./mvnw clean verify
```

### Run

```bash
./mvnw spring-boot:run
```

The service will start on `http://localhost:8080`

### Example Request

```bash
curl "http://localhost:8080/api/v1/repositories/rank?language=Java&createdAfter=2022-01-01&page=1&limit=20"
```

## Scoring Formula

The service ranks repositories by a composite score combining three signals:

```
finalScore = 100 × (0.60 × starComponent + 0.25 × forkComponent + 0.15 × recencyComponent)
```

### Star Component (60% weight)

Stars represent public popularity. Uses logarithmic scaling to prevent extremely popular repositories from dominating:

```
starComponent = min(1, ln(1 + stars) / ln(1 + 100000))
```

### Fork Component (25% weight)

Forks represent stronger engagement (reuse, modification, contribution). Also logarithmic:

```
forkComponent = min(1, ln(1 + forks) / ln(1 + 20000))
```

### Recency Component (15% weight)

Measures recent activity using half-life decay. A repository updated 180 days ago scores 50% of this component:

```
ageDays = max(0, daysUntilNow(updatedAt))
recencyComponent = exp(-ln(2) × ageDays / 180)
```

## Configuration

Configure via `application.yml`:

```yaml
ranking:
  score-version: v1
  
  weights:
    stars: 0.60
    forks: 0.25
    recency: 0.15
  
  stars:
    reference-maximum: 100000
  
  forks:
    reference-maximum: 20000
  
  recency:
    half-life-days: 180

cache:
  ranking:
    ttl: 5m
    maximum-size: 500

github:
  base-url: https://api.github.com
  api-version: "2026-03-10"
  token: ${GITHUB_TOKEN:}
  connect-timeout: 2s
  response-timeout: 5s
  maximum-page-size: 50
  retry:
    max-attempts: 3

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Optional GitHub Token

Set the `GITHUB_TOKEN` environment variable to use authenticated API requests, which usually provide higher GitHub API limits than unauthenticated requests:

```bash
export GITHUB_TOKEN=your_github_token
./mvnw spring-boot:run
```

Without a token, the service uses unauthenticated requests and is more likely to encounter GitHub rate limits.

## API

### GET /api/v1/repositories/rank

Ranks repositories by composite score.

Rank values are relative to the candidate page returned by GitHub for the requested `page` and `limit`. They are not global ranks across all repositories matching the query.

**Query Parameters:**
- `language` (required): Programming language (e.g., `Java`, `Python`)
- `createdAfter` (required): Earliest creation date in ISO format (e.g., `2022-01-01`)
- `page` (optional, default=1): Page number (1-indexed)
- `limit` (optional, default=20): Results per page (1-50)
- `scoreVersion` (optional, default=v1): Ranking algorithm version

**Response (200):**

```json
{
  "query": {
    "language": "Java",
    "createdAfter": "2022-01-01",
    "page": 1,
    "limit": 20
  },
  "scoreVersion": "v1",
  "generatedAt": "2026-07-15T15:00:00Z",
  "pagination": {
    "page": 1,
    "limit": 20,
    "returnedCount": 20,
    "githubTotalCount": 12345,
    "incompleteResults": false
  },
  "repositories": [
    {
      "id": 123,
      "name": "example",
      "fullName": "owner/example",
      "url": "https://github.com/owner/example",
      "description": "Example repository",
      "language": "Java",
      "createdAt": "2022-03-01T10:00:00Z",
      "updatedAt": "2026-07-14T10:00:00Z",
      "stars": 25000,
      "forks": 4100,
      "score": {
        "total": 74.36,
        "starsContribution": 45.12,
        "forksContribution": 17.31,
        "recencyContribution": 11.93
      },
      "rank": 1
    }
  ]
}
```

#### Example cURL request

```sh
curl -G "http://localhost:8080/api/v1/repositories/rank" \
  --data-urlencode "language=Java" \
  --data-urlencode "createdAfter=2022-01-01" \
  --data-urlencode "page=1" \
  --data-urlencode "limit=20" \
  --data-urlencode "scoreVersion=v1" \
  -H "Accept: application/json" \
  -H "User-Agent: repository-ranking-client/1.0"
```

### GET /api/v1/health

Health check endpoint.

### GET /api/v1/health/readiness

Readiness check endpoint.

## Error Handling

Errors follow RFC 7807 Problem Details format:

```json
{
  "type": "https://example.com/problems/invalid-request",
  "title": "Invalid request",
  "status": 400,
  "detail": "createdAfter must use ISO date format yyyy-MM-dd",
  "instance": "/api/v1/repositories/rank",
  "errorCode": "INVALID_CREATED_AFTER",
  "timestamp": "2026-07-15T15:00:00Z",
  "correlationId": "d0e79977-4afe-48ea-9eb0-14731ae773f6"
}
```

**HTTP Status Codes:**
- `200`: Ranked repositories returned (or empty result)
- `400`: Invalid request parameter
- `429`: GitHub rate limit exceeded (includes `retryAfter` in response)
- `502`: Invalid or unexpected GitHub response
- `503`: GitHub timeout or unavailable
- `500`: Unexpected internal error

## Caching

Successful ranking results are cached in a thread-safe bounded Caffeine cache for 5 minutes by default. Cache key includes:
- Language
- Created after date
- Page number
- Limit
- Score version

Cache is per-instance and stored in memory. TTL and maximum size are enforced by Caffeine. For distributed deployments, configure a shared cache backend (e.g., Redis).

## GitHub Integration

The service uses only the GitHub `/search/repositories` endpoint:

- Single API call per uncached request
- No additional enrichment calls
- No commit history, contributor, or security data
- Handles GitHub rate-limit responses returned as `429` or as `403` with `X-RateLimit-Remaining: 0`
- Uses `Retry-After` or `X-RateLimit-Reset` when GitHub provides retry metadata

## Limitations

### By Design

This version intentionally does **not** score by:
- Commit frequency or history
- Contributor count or activity
- Community health metrics
- Security scorecard
- Release history
- Issue count or resolution

The score is **not** intended to measure:
- Code quality
- Security posture
- Documentation quality
- Project maturity or sustainability
- Production-readiness

It answers one question: **Which repositories are the strongest combination of public popularity, engagement, and recent activity?**

### Known Limitations

1. **GitHub Rate Limiting**: GitHub Search API requests have specific rate limits that may differ from general REST API limits. The service maps primary and secondary rate limits to local `429` responses when GitHub returns `429` or `403` with no remaining quota.
2. **In-Memory Cache**: Cache is per-instance; distributed deployments need shared cache.
3. **Incomplete Results**: GitHub search may return `incomplete_results: true` for very broad queries.
4. **Accessibility**: Highest page accessible depends on GitHub's search API limits (~1000 total results).
5. **Page-Local Ranking**: The service re-ranks only the candidate page returned by GitHub, not every repository matching the query globally.

## Testing

Run all tests:

```bash
./mvnw test
```

Run specific test class:

```bash
./mvnw test -Dtest=RepositoryScorerTest
```

### Test Categories

- **Unit tests**: Scoring logic, ranking service orchestration, validation, and cache behavior
- **MVC tests**: Controller validation, successful responses, and stable error responses with mocked service boundaries
- **HTTP client tests**: GitHub client communication with `MockRestServiceServer`
- **Contract tests**: Generated OpenAPI availability plus static contract path/error-schema checks

Tests require no GitHub token and make no real GitHub API calls.

## OpenAPI Specification

The service exposes an OpenAPI 3.0 specification at:

```
http://localhost:8080/api-docs
```

Interactive Swagger UI (if enabled):

```
http://localhost:8080/swagger-ui.html
```

## Implementation Notes

### API Contract

The static API contract is kept in `src/main/resources/openapi.yaml`, and tests verify that key documented paths match runtime endpoints.

### Tested Scoring

The scoring model is covered by deterministic unit tests:
- Star component tests
- Fork component tests
- Recency component tests
- Final score validation

### Deterministic Ranking

Ranking is deterministic with stable tie-breaking:
1. Raw internal score (descending; displayed score remains rounded)
2. Stars (descending)
3. Forks (descending)
4. Updated date (descending)
5. Full repository name (ascending)

Same input always produces same page-local ranking.

### Immutable Domain Models

All domain objects are records (immutable):
- `RepositoryCandidate`
- `RankedRepository`
- `RankingRequest`
- `RankingResult`
- `ScoreBreakdown` (contains raw internal score plus rounded displayed score)

### Separation of Concerns

- **Domain**: Business logic independent of frameworks
- **Integration**: GitHub HTTP DTOs separate from domain
- **API**: Response DTOs separate from domain
- **Service**: Orchestration layer
- **Config**: Externalized, validated at startup

## Development

### Build and verify:

```bash
./mvnw clean verify
```

## Local development environment (asdf)

This project targets Java 21 and uses Maven. If you use `asdf` to manage SDKs and tool versions, add the repository root to your asdf-managed projects by creating a `.tool-versions` file (already included) and installing the required plugins.

Example (one-time setup):

```bash
# install asdf following https://asdf-vm.com/
asdf plugin-add java
asdf plugin-add maven
asdf install
asdf current
```

The included `.tool-versions` requests Java 21 and Maven 3.9.4. Adjust versions if you prefer a specific distribution (e.g. Temurin).

If you don't use `asdf` you can still build with the included Maven wrapper (`./mvnw`).

## Docker

You can build and run the application using Docker. A multi-stage `Dockerfile` is provided which compiles the application with Maven and packages a runnable JAR.

Build the image:

```bash
docker build -t repository-ranking-service:latest .
```

Run the container:

```bash
docker run --rm -p 8080:8080 repository-ranking-service:latest
```

The Docker build uses public base images and runs the Maven verification suite before creating the runtime image. The service will be available on `http://localhost:8080`.

If you prefer a quick local build without Docker:

```bash
./mvnw clean package
java -jar target/*.jar
```

## Dependencies

- Spring Boot 3.2.0
- Spring Web
- Caffeine (bounded in-memory cache)
- SpringDoc OpenAPI (API documentation)
- Spring MVC test and `MockRestServiceServer` (testing)
- AssertJ (assertions)
