# MuttCUES-API

Spring Boot 3.2.3 / Java 21 image processing API. Handles uploads, async job processing, DDS conversion, and upscaling via an external Upscayl microservice.

## Commands

```bash
mvn clean package -DskipTests   # Build JAR
mvn test                         # Run all tests
mvn spring-boot:run              # Run locally (port 8080)
make dev                         # Docker dev environment
make prod                        # Docker production environment
make test-api                    # API tests in Docker
make health                      # Check service health
```

Tests: JUnit 5 in `src/test/java/net/muttcode/spring/`. Method naming: `methodName_condition_expectedResult`.

## Architecture

```
src/main/java/net/muttcode/spring/
├── controller/
│   ├── AuthController.java
│   ├── FileController.java          # Upload/download; 200MB limit
│   ├── JobController.java           # Job status polling
│   ├── DdsConversionController.java
│   └── UpscaylHealthController.java
├── service/
│   ├── JobQueueService.java         # Async job queue (@Async)
│   ├── FileService.java
│   ├── ImageProcessingService.java
│   ├── UpscaylService.java          # Delegates to http://upscayl:8081
│   ├── DdsConversionService.java
│   ├── ProcessingJobService.java
│   └── JwtService.java
├── model/
│   ├── ProcessingJob.java           # Job entity (UUID PK, status, type)
│   ├── File.java / StoredFile.java / ProcessedFile.java
│   ├── User.java
│   └── ApiKey.java
├── repository/                      # JPA repositories
├── config/
│   └── SecurityConfig.java          # JWT auth, CORS — update when adding endpoints
└── FileApiApplication.java
```

## Conventions

- Constructor injection; no `@Autowired` on fields
- SLF4J logging: `private static final Logger logger = LoggerFactory.getLogger(Class.class)`
- JPA entities use Lombok `@Data`, UUID PKs (`@GeneratedValue(strategy = GenerationType.UUID)`)
- Auth filter chain: `JwtAuthenticationFilter` → `JwtService` → `SecurityConfig`
- Update `SecurityConfig.java` whenever adding endpoints with different auth requirements
- PostgreSQL for persistence, Redis for caching
- Docker compose: `docker-compose.yml` (base), `docker-compose.dev.yml`, `docker-compose.prod.yml`
