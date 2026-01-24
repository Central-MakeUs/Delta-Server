# delta: Agent Playbook

This file is for agentic coding tools operating in this repository.

## Project

- Stack: Java 17, Spring Boot 3.x, Gradle (wrapper), JUnit 5
- Architecture: Hexagonal (Ports & Adapters)
- Entry point: `src/main/java/cmc/delta/DeltaApplication.java`

Observed versions:

- Spring Boot plugin: 3.5.7 (`build.gradle`)
- Gradle wrapper: 9.2.1 (`gradle/wrapper/gradle-wrapper.properties`)

## Quick Commands (Local)

Use the Gradle wrapper from repo root.

- List tasks: `./gradlew tasks --all`
- Build (jar): `./gradlew clean bootJar`
- Build (full): `./gradlew clean build`
- Run app: `./gradlew bootRun`
- Run app w/ test classpath: `./gradlew bootTestRun`
- Run all checks (includes formatting check): `./gradlew check`
- Run tests: `./gradlew test`
- Inspect dependencies: `./gradlew dependencies`

### Run A Single Test

Gradle supports filtering via `--tests`.

- One test class:
  - `./gradlew test --tests "cmc.delta.domain.user.application.service.UserServiceImplTest"`
- One test method:
  - `./gradlew test --tests "cmc.delta.domain.user.application.service.UserServiceImplTest.getMyProfile_ok_returnsUser"`
- Wildcards:
  - `./gradlew test --tests "cmc.delta.domain.*.*Test"`

Useful test options:

- Fail fast: `./gradlew test --fail-fast`
- Debug JVM: `./gradlew test --debug-jvm`

## Lint / Format

Formatting is enforced via Spotless.

- Check formatting: `./gradlew spotlessCheck`
- Auto-fix formatting: `./gradlew spotlessApply`

Notes:

- `./gradlew spotlessCheck` currently reports formatting violations across many files; run `spotlessApply` before committing changes.
- `build.gradle` config references `$rootDir/tools/naver-eclipse-formatter.xml`, but `tools/` is gitignored (`.gitignore` contains `/tools/`). In this checkout, `tools/` is not present on disk; do not assume formatter config files exist.
- If Spotless fails on import order or wrapping, do not “fight” it; run `spotlessApply` and commit the result.

## Build Artifacts

- Spring Boot jar output: `build/libs/*.jar` (from `bootJar`).
- Container image build task exists: `./gradlew bootBuildImage` (uses Spring Boot buildpacks).

## CI / Docker

- Docker build uses Gradle wrapper and produces `build/libs/*.jar` (see `Dockerfile`).
- There is a SonarCloud workflow stub at `.github/workflows/sonarcloud.yml` (currently commented out) showing an intended command: `./gradlew sonar --info`.

## Repo Layout (Hexagonal)

Top-level packages:

- `cmc.delta.domain.<feature>`: feature modules
- `cmc.delta.global`: cross-cutting concerns (security, error handling, logging, storage, swagger, config)

Common feature structure (examples are in `src/main/java/cmc/delta/domain/`):

- `adapter/in/**`: inbound adapters
  - Web controllers live under `adapter/in/web/**` (REST)
  - Workers/schedulers live under `adapter/in/worker/**`
- `adapter/out/**`: outbound adapters (persistence, external APIs, S3, OAuth)
- `application/port/in/**`: inbound ports (use cases)
- `application/port/out/**`: outbound ports (repositories, external clients)
- `application/service/**`: use case implementations (often `*Impl`)
- `application/validation/**` or `**/validator/**`: request/domain validation
- `model/**`: domain entities/enums/value objects

When adding new functionality:

- Prefer defining behavior at a port (interface) and implementing it behind an adapter.
- Keep controllers thin: bind/validate inputs, call a use case, return DTO.
- Keep outbound integrations behind `port/out` + `adapter/out`.

## Code Style (Observed + Enforced)

Follow Spotless output; do not hand-format around it.

- Indentation: tabs are used widely in existing code (see `SecurityConfig.java`, `GlobalExceptionHandler.java`).
- Imports:
  - Let Spotless reorder imports; avoid "manual" import grouping.
  - Tests often use static AssertJ imports (e.g., `import static org.assertj.core.api.Assertions.*;`).
- Prefer Java 17 language features where already used:
  - DTOs are frequently `record`s.
  - Keep DTOs small and immutable.
- Lombok is used in entities/config classes (`@RequiredArgsConstructor`, `@Getter`, `@NoArgsConstructor`).
- Naming:
  - Ports: `*Port` (outbound) and `*UseCase` (inbound)
  - Implementations: `*Impl`
  - Validators: `*Validator`
  - Properties/config: `*Properties`, `*Config`

## Error Handling + API Responses

- Throw `BusinessException` (or subtype) with an `ErrorCode` for domain/business failures.
- Global exception translation is handled in `GlobalExceptionHandler`.
- API responses are wrapped with `ApiResponse<T>`; use helpers in `ApiResponses`.

Guideline:

- Do not leak sensitive details in 5xx responses (see `GlobalExceptionHandler.shouldHideDetail`).

## Testing Conventions

- Frameworks: JUnit 5 (`useJUnitPlatform()`), AssertJ, Mockito.
- Prefer descriptive `@DisplayName`.
- Prefer AssertJ `assertThat(...)` and `catchThrowableOfType(...)` for exception assertions.
- Put fakes/fixtures under `src/test/java/**/support/**` when helpful (pattern exists in repo).

Web tests:

- Web controllers have WebMvc tests under `src/test/java/**/adapter/in/web/**`.

## Configuration / Secrets

- Do not commit secrets. `*.env` is ignored by git.
- Use `.env.example` as the source of required env vars.
- Spring config is in `src/main/resources/application*.yml` and reads env vars like `JWT_SECRET_BASE64`, `MATHPIX_APP_ID`, `GEMINI_API_KEY`, etc.
- Logs are written under `logs/` (gitignored) and may include sensitive request context; avoid copying logs into PRs.

## Logging

- Use SLF4J (`LoggerFactory.getLogger(...)`) and structured placeholders (`log.info("x={} y={}", x, y)`).
- HTTP access logging exists (`HttpAccessLogFilter`) and intentionally excludes query strings to avoid leaking sensitive data.

## Cursor / Copilot Rules

- No Cursor rules found in `.cursor/rules/` or `.cursorrules`.
- No Copilot instructions found at `.github/copilot-instructions.md`.
