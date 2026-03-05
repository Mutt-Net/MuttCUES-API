# PROJECT_SPEC.md - MuttCUES-API

## Project Name
MuttCUES-API

## Description
MuttCUES-API is a Spring Boot-based image processing platform that provides AI-powered image upscaling using Real-ESRGAN (via upscayl-ncnn), DDS texture conversion for game development, and a comprehensive job queue system for asynchronous processing. The API orchestrates image processing workflows with PostgreSQL for persistence, Redis for caching, and integrates with a separate upscayl microservice for GPU-accelerated upscaling operations.

## Audience
- Game developers needing texture upscaling and DDS conversion
- Digital artists requiring AI-powered image enhancement
- Development teams integrating image processing into their applications
- Users of the MuttCUES platform (paired with MuttCUES-FE React frontend)

## Jobs To Be Done (JTBD)
1. **Upload and upscale images** - Users can upload images and have them AI-upscaled by 2x, 4x, or other scale factors using Real-ESRGAN models
2. **Convert DDS textures** - Game developers can convert between DDS and PNG formats for game texture management
3. **Track processing jobs** - Users can submit long-running image processing jobs asynchronously and monitor their status until completion

## Technical Constraints
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.3
- **Build Tool**: Maven 3.9
- **Database**: PostgreSQL with JPA/Hibernate
- **Cache/Queue**: Redis (Spring Data Redis)
- **Security**: Spring Security with JWT authentication (io.jsonwebtoken jjwt 0.12.5)
- **Containerization**: Docker (multi-stage builds)
- **Runtime**: Eclipse Temurin 21 JRE
- **External Services**: Upscayl-NCNN microservice for image processing
- **Max File Upload**: 200MB (configured in application.properties)

## Build/Test Commands

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Run Application (Development)
```bash
mvn spring-boot:run
```

### Docker Build
```bash
docker build -f Dockerfile.api -t muttcues-api:latest .
```

### Docker Compose (Development)
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

### Docker Compose (Production)
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Makefile Commands
```bash
make dev              # Start development environment
make prod             # Start production environment
make test-api         # Run API tests in container
make build            # Build all Docker services
make health           # Check service health
```

## Architecture

### High-Level Architecture
```
┌─────────────────┐
│   Frontend      │  (React + Vite, Port 5173/3000)
│   (Nginx)       │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API Server    │  (Spring Boot, Port 8080)
│  (This Project) │
└────┬───────┬────┘
     │       │
     ▼       ▼
┌─────────┐ ┌──────────┐
│Postgres │ │ Upscayl  │
│(JPA)    │ │ Service  │
└─────────┘ └──────────┘
     │
     ▼
┌─────────┐
│  Redis  │
│ (Cache) │
└─────────┘
```

### Package Structure
```
src/main/java/net/muttcode/spring/
├── FileApiApplication.java      # Main entry point
├── config/
│   ├── CorsConfig.java          # CORS configuration
│   ├── JwtAuthenticationFilter.java  # JWT request filter
│   └── SecurityConfig.java      # Spring Security config
├── controller/
│   ├── AuthController.java      # Authentication endpoints
│   ├── DdsConversionController.java  # DDS conversion API
│   ├── FileController.java      # File upload/download API
│   └── JobController.java       # Job queue management API
├── model/
│   ├── ProcessedFile.java       # JPA entity for processed files
│   ├── ProcessingJob.java       # JPA entity for processing jobs
│   └── User.java                # JPA entity for users
├── repository/
│   ├── ProcessingJobRepository.java  # Spring Data JPA repository
│   └── UserRepository.java      # User repository
└── service/
    ├── CustomUserDetailsService.java  # Spring Security user details
    ├── DdsConversionService.java      # DDS conversion logic
    ├── FileService.java               # File storage/retrieval
    ├── ImageProcessingService.java    # Main processing orchestration
    ├── JobQueueService.java           # Redis-backed job queue
    ├── JwtService.java                # JWT token generation/validation
    ├── ProcessingJobService.java      # Job CRUD operations
    ├── StoredFile.java                # File metadata handling
    └── UpscaylService.java            # Upscayl microservice client
```

### Key Components
1. **Controllers**: REST API endpoints for file upload, job submission, and status queries
2. **Services**: Business logic for image processing, job orchestration, and external service integration
3. **Repositories**: Spring Data JPA interfaces for database access
4. **Security**: JWT-based authentication with Spring Security filter chain
5. **Async Processing**: `@EnableAsync` with `@Async` methods for background job processing

### Data Flow (Image Upscaling)
1. Client uploads image via `/api/upload` endpoint
2. File stored, metadata saved to PostgreSQL `files` table
3. Client submits upscaling job via `/api/jobs/process`
4. Job created in `processing_jobs` table with status "queued"
5. `ImageProcessingService.processJobAsync()` picks up job
6. Input file copied to upscayl mount point
7. UpscaylService calls upscayl microservice via HTTP
8. Processed image retrieved and stored
9. Job status updated to "completed" with output file reference

### Database Schema (init-db.sql)
- `files` - Uploaded file metadata
- `processed_files` - Processing results
- `processing_jobs` - Job queue with status tracking
- `users` - User authentication (future use)
- `api_keys` - API key management (future use)
- `usage_statistics` - Analytics and monitoring
