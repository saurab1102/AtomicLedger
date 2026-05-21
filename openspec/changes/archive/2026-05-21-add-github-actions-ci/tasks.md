## 1. GitHub Actions Workflow

- [x] 1.1 Add a GitHub Actions workflow that runs on `push` and `pull_request`.
- [x] 1.2 Configure the workflow to use Java 21 and cache Maven dependencies.
- [x] 1.3 Run `./mvnw test` in the workflow and keep the existing Testcontainers-based integration tests working on GitHub-hosted runners.

## 2. Repository Documentation

- [x] 2.1 Add a CI status badge to the README that points at the GitHub Actions workflow.
- [x] 2.2 Keep the change operational only and avoid modifying application behavior.

## 3. Verification

- [x] 3.1 Validate the workflow configuration and confirm the repository still uses `./mvnw test` as the canonical verification command.
- [x] 3.2 Add a lightweight check or documentation note that CI is expected to support the Testcontainers-based test suite on GitHub-hosted runners.
