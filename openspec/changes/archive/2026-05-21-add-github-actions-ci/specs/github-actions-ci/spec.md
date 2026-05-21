## ADDED Requirements

### Requirement: Run CI on pushes and pull requests
The system SHALL include a GitHub Actions workflow that runs on both `push` and `pull_request`.

#### Scenario: Push triggers CI
- **WHEN** a commit is pushed to the repository
- **THEN** the GitHub Actions CI workflow starts automatically

#### Scenario: Pull request triggers CI
- **WHEN** a pull request is opened or updated
- **THEN** the GitHub Actions CI workflow starts automatically

### Requirement: Verify the project with Java 21 and Maven caching
The GitHub Actions workflow SHALL use Java 21 and SHALL cache Maven dependencies while running the project test suite with `./mvnw test`.

#### Scenario: CI runs Maven tests with Java 21
- **WHEN** the workflow executes on a GitHub runner
- **THEN** it provisions Java 21, restores or saves Maven dependency cache entries, and runs `./mvnw test`

### Requirement: Support Testcontainers-based integration tests in CI
The GitHub Actions workflow SHALL run in an environment where the existing Testcontainers-based integration tests can execute successfully.

#### Scenario: Testcontainers suite runs in CI
- **WHEN** the workflow executes the Maven test suite
- **THEN** the Testcontainers-based PostgreSQL integration tests are able to start and run as part of CI

### Requirement: Expose CI status in the README
The repository SHALL display a GitHub Actions CI badge in the README.

#### Scenario: README shows workflow status
- **WHEN** a repository visitor views the README
- **THEN** the README includes a badge that reflects the status of the GitHub Actions CI workflow
