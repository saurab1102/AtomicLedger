## Why

AtomicLedger currently relies on local test runs to catch regressions, which means branch and pull request quality depends on manual discipline. Adding GitHub Actions CI now will give the project automatic feedback on pushes and pull requests while keeping the application behavior unchanged.

## What Changes

- Add a GitHub Actions workflow that runs on `push` and `pull_request`.
- Use Java 21 in CI and cache Maven dependencies for faster builds.
- Run `./mvnw test` as the main verification step.
- Ensure the existing Testcontainers-based test suite can run in GitHub-hosted CI.
- Add a CI status badge to the README.
- Keep all application runtime behavior unchanged.

## Capabilities

### New Capabilities
- `github-actions-ci`: Defines automated GitHub Actions verification for the project, including trigger conditions, Java/Maven setup, test execution, and CI visibility through the README badge.

### Modified Capabilities

## Impact

- Affected files will include a GitHub Actions workflow and the project README.
- CI will exercise the existing Maven and Testcontainers-based test flow in GitHub-hosted runners.
- No domain logic, APIs, or application behavior will change.
