# AGENTS.md - Stratum Configuration for MuttCUES-API

## Build Command
```bash
mvn clean package -DskipTests
```

## Test Command
```bash
mvn test
```

## Lint Command
None configured. Project uses standard Maven build without explicit linting. Code style is enforced through IDE conventions and Spring Boot best practices.

## Project Structure
```
MuttCUES-API/
├── src/
│   ├── main/
│   │   ├── java/net/muttcode/spring/
│   │   │   ├── FileApiApplication.java
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── DdsConversionController.java
│   │   │   │   ├── FileController.java
│   │   │   │   └── JobController.java
│   │   │   ├── model/
│   │   │   │   ├── ProcessedFile.java
│   │   │   │   ├── ProcessingJob.java
│   │   │   │   └── User.java
│   │   │   ├── repository/
│   │   │   │   ├── ProcessingJobRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   └── service/
│   │   │       ├── CustomUserDetailsService.java
│   │   │       ├── DdsConversionService.java
│   │   │       ├── FileService.java
│   │   │       ├── ImageProcessingService.java
│   │   │       ├── JobQueueService.java
│   │   │       ├── JwtService.java
│   │   │       ├── ProcessingJobService.java
│   │   │       ├── StoredFile.java
│   │   │       └── UpscaylService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/java/net/muttcode/spring/
│       ├── controller/
│       │   └── JobControllerTest.java
│       └── service/
│           ├── JwtServiceTest.java
│           └── ProcessingJobServiceTest.java
├── .github/
│   └── workflows/
│       ├── docker-api.yml
│       └── docker-upscayl.yml
├── backups/
├── .env.example
├── .gitignore
├── .classpath
├── .dockerignore
├── .factorypath
├── .project
├── DEPLOYMENT_SUMMARY.md
├── DOCKER_README.md
├── Dockerfile.api
├── LICENSE
├── Makefile
├── pom.xml
├── prometheus.yml
├── init-db.sql
├── PROJECT_SPEC.md (this file)
└── AGENTS.md
```

## Coding Conventions

### Naming Conventions
- **Classes**: PascalCase (e.g., `ImageProcessingService`, `JwtAuthenticationFilter`)
- **Methods**: camelCase (e.g., `processJobAsync`, `generateToken`)
- **Variables**: camelCase (e.g., `jobId`, `scaleFactor`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `logger`, `UPSCAYL_INPUT_MOUNT`)
- **Test Classes**: Suffix with `Test` (e.g., `JwtServiceTest`)
- **Test Methods**: Descriptive camelCase (e.g., `generateToken_shouldCreateValidToken`)

### Code Style
- **Indentation**: Tab character (Eclipse/STS default)
- **Braces**: K&R style (opening brace on same line)
- **Line Length**: No strict limit, but ~120 chars preferred
- **Imports**: Organized by package (java, javax, org, com, net)
- **Annotations**: Spring annotations on same line as declaration

### Spring Boot Patterns
- **Dependency Injection**: Constructor injection preferred (see `ImageProcessingService`)
- **Configuration**: `@Value` for properties, `@ConfigurationProperties` for grouped settings
- **Exception Handling**: Try-catch with logging via SLF4J
- **Async Processing**: `@Async` annotation with `@EnableAsync` on main class
- **REST Controllers**: `@RestController` with explicit `@RequestMapping` paths

### Testing Conventions
- **Framework**: JUnit 5 (Jupiter) via `spring-boot-starter-test`
- **Test Setup**: `@BeforeEach` for initialization
- **Assertions**: JUnit 5 assertions (`assertEquals`, `assertTrue`, `assertNotNull`)
- **Mocking**: Spring's `ReflectionTestUtils` for setting private fields in tests
- **Test Method Naming**: `methodName_condition_expectedResult` pattern
  - Example: `validateToken_shouldReturnTrueForValidToken`
  - Example: `generateToken_withRole_shouldIncludeRole`

### Logging
- **Framework**: SLF4J with Logback (Spring Boot default)
- **Pattern**: `private static final Logger logger = LoggerFactory.getLogger(Class.class)`
- **Levels**: `info` for normal operations, `error` for failures, `debug` for detailed tracing

### Security Patterns
- **JWT**: Token-based authentication with `JwtAuthenticationFilter`
- **Password Encoding**: BCrypt via `PasswordEncoder` bean
- **CORS**: Explicit configuration in `SecurityConfig` with allowed origins
- **Stateless**: Session creation policy set to `STATELESS`

### Database Patterns
- **JPA Entities**: Lombok `@Data`, `@Entity`, `@Table` annotations
- **UUID Primary Keys**: `@GeneratedValue(strategy = GenerationType.UUID)`
- **Timestamps**: `@CreationTimestamp`, `@UpdateTimestamp` or manual `CURRENT_TIMESTAMP`
- **JSON Columns**: `@Type(JsonBinaryType.class)` for JSONB fields

### Docker Conventions
- **Multi-stage Builds**: Builder stage (Maven) → Runtime stage (JRE)
- **Base Images**: `maven:3.9-eclipse-temurin-21` for build, `eclipse-temurin:21-jre-jammy` for runtime
- **Non-root User**: `appuser` created for security
- **Health Checks**: HTTP endpoint check via `/actuator/health`
- **Environment Variables**: `JAVA_OPTS` for JVM tuning

### API Design
- **RESTful Paths**: `/api/{resource}`, `/api/{resource}/{id}`
- **HTTP Methods**: GET (read), POST (create), PUT (update), DELETE (remove)
- **Response Format**: JSON via Jackson serialization
- **Error Handling**: `spring.error.include-message=always` for debugging
- **File Uploads**: Multipart with 200MB limit

### Git Workflow
- **Branches**: `main` (stable), `develop` (integration)
- **CI/CD**: GitHub Actions for Docker builds on push/tag
- **Tags**: Semantic versioning (`v{major}.{minor}.{patch}`)
- **Commits**: Conventional commits preferred

## Common Development Tasks

### Add New Endpoint
1. Create method in appropriate controller (e.g., `FileController.java`)
2. Add service method for business logic
3. Update `SecurityConfig.java` if authentication requirements differ
4. Add test in corresponding `*Test.java`

### Add New Entity
1. Create class in `model/` package with JPA annotations
2. Create repository interface in `repository/` package
3. Add table to `init-db.sql` for manual deployments
4. Update schema if using auto-ddl

### Add Configuration Property
1. Add to `application.properties` with `prefix.property` format
2. Inject via `@Value("${property.name:default}")` or create `@ConfigurationProperties` class
3. Document in `.env.example`

### Run Full Test Suite
```bash
mvn clean test
```

### Build for Production
```bash
mvn clean package
docker build -f Dockerfile.api -t muttcues-api:latest .
```

### Debug in Container
```bash
docker-compose exec api /bin/bash
# Then inspect logs, files, or run commands
```

---

## Git Workflow

### Branch Structure

```
main (protected)
  └── dev
        └── feature/short-name
        └── fix/short-name
```

Merge flow: `feature/*` or `fix/*` → `dev` → `main`

### Commit Format

```
type(scope): subject

Body (72-char wrap). Explain why, not what.
Resolves TASK-NNN from IMPLEMENTATION_PLAN.md (if applicable).

-MuttNET-
```

**Types:** `feat` `fix` `docs` `chore` `refactor` `test` `perf` `ci` `build`

### Signature

End every commit with `-MuttNET-` on its own line. This is the
provenance marker for Holly-assisted commits.

### Pre-commit Hooks

Installed via `Z:/holly-state/scripts/install-hooks.sh`.
Direct commits to `main` are blocked — use `dev` as integration branch.

### Full Spec

`Z:/holly-state/docs/commit-standards.md`
