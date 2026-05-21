## Context

AtomicLedger already has a strong automated test signal locally through `./mvnw test`, including Testcontainers-backed integration tests against PostgreSQL. What is missing is a shared, automatic verification path in the repository so pushes and pull requests are checked consistently before merge.

This change is operational rather than domain-facing: it adds GitHub Actions CI, wires it to the existing Maven wrapper test command, and exposes CI status through the README without changing application behavior.

## Goals / Non-Goals

**Goals:**
- Add a GitHub Actions workflow that runs on `push` and `pull_request`.
- Use Java 21 in CI to match the project’s runtime/build target.
- Cache Maven dependencies to reduce repeated CI latency.
- Run `./mvnw test` as the canonical CI verification command.
- Ensure the Testcontainers-based integration suite can run on GitHub-hosted runners.
- Surface CI status in the README with a badge.

**Non-Goals:**
- Changing application code paths, API behavior, or domain logic.
- Splitting tests into multiple CI jobs or introducing a complex matrix.
- Adding deployment, release, or packaging automation.
- Replacing Testcontainers with a separate manually managed database service.

## Decisions

### Use a single GitHub Actions workflow for pushes and pull requests
The CI setup should start with one straightforward workflow that covers the repository’s main test signal.

Rationale:
- The project’s current verification flow is centered on `./mvnw test`.
- A single workflow is easier to reason about and maintain while the codebase is still compact.

Alternative considered:
- Separate workflows for PRs vs pushes. Rejected because the current requirements and test surface do not justify the extra complexity.

### Use GitHub-hosted Ubuntu runners with `actions/setup-java` Maven caching
The workflow should use Java 21 on a standard GitHub-hosted Linux runner and rely on built-in Maven dependency caching from `actions/setup-java`.

Rationale:
- This matches the project’s current Java 21 target.
- It keeps dependency caching declarative and avoids custom cache key logic unless needed later.

Alternative considered:
- Manual `actions/cache` configuration. Rejected because `setup-java` already supports Maven caching with less boilerplate.

### Run the existing Testcontainers tests directly in CI
The workflow should run `./mvnw test` without replacing the current PostgreSQL Testcontainers setup with a service container.

Rationale:
- GitHub-hosted runners provide Docker, which Testcontainers can use directly.
- Reusing the local test path keeps CI aligned with developer workflows and reduces environment drift.

Alternative considered:
- Provision a dedicated PostgreSQL service in the workflow. Rejected because the tests are already written around Testcontainers and do not need a separate CI-only database wiring.

### Add the badge to the README using the workflow name
The README badge should point at the new GitHub Actions workflow so repository visitors can see CI status immediately.

Rationale:
- The badge is lightweight, visible, and directly tied to the repository’s health signal.
- It keeps CI discoverable without requiring people to navigate into the Actions tab first.

## Risks / Trade-offs

- [Risk] Testcontainers can fail in CI if Docker availability or permissions differ from local assumptions. → Mitigation: use GitHub-hosted Linux runners, which provide Docker support compatible with Testcontainers.
- [Risk] First-run CI times may still be slower despite caching because Maven and Docker layers need warm-up. → Mitigation: enable Maven dependency caching and keep the initial workflow focused on the single existing test command.
- [Risk] README badges require the correct repository/workflow path and can break if the workflow is renamed later. → Mitigation: use a stable workflow filename/name and keep the badge tied to that canonical workflow.

## Migration Plan

1. Add the GitHub Actions workflow under `.github/workflows/`.
2. Configure Java 21 and Maven dependency caching.
3. Run `./mvnw test` in CI and rely on GitHub-hosted Docker support for Testcontainers.
4. Add a CI badge to the README.

Rollback strategy:
- Remove the workflow file and README badge if the CI setup causes unacceptable maintenance overhead or runner incompatibility.

## Open Questions

- No open questions for this scope. The required triggers, Java version, cache behavior, Maven command, Testcontainers expectation, and README badge are sufficiently specific to implement directly.
