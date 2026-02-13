# MuttCUES Docker Deployment Guide

Complete Docker containerization for the MuttCUES image processing platform.

## 📋 Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

## 🎯 Overview

MuttCUES is a comprehensive image processing platform with:
- **Image Upscaling** using AI-powered Real-ESRGAN (upscayl-ncnn)
- **DDS Conversion** for game texture management
- **Spring Boot API** for processing orchestration
- **React Frontend** for user interface

## ✅ Prerequisites

### Required
- Docker Engine 24.0+ ([Install Docker](https://docs.docker.com/get-docker/))
- Docker Compose 2.20+ ([Install Compose](https://docs.docker.com/compose/install/))
- 8GB RAM minimum (16GB recommended)
- 20GB free disk space

### Optional (for GPU acceleration)
- NVIDIA GPU with Vulkan support
- [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html)

### Recommended
- Linux host (Ubuntu 22.04/24.04 preferred)
- Docker with BuildKit enabled

## 🚀 Quick Start

### 1. Clone and Setup

```bash
# Clone the repositories
git clone https://github.com/Mutt-Net/MuttCUES-API.git
git clone https://github.com/Mutt-Net/MuttCUES-FE.git

# Copy Docker files to project root
cp Dockerfile.* MuttCUES-API/
cp Dockerfile.frontend MuttCUES-FE/
cp nginx.conf MuttCUES-FE/
cp docker-compose*.yml ./
cp .env.example .env

# Edit configuration
nano .env
```

### 2. Build Upscayl Binary

```bash
# Build the upscayl-ncnn Docker image
docker build -f Dockerfile.upscayl -t muttcues-upscayl:latest .

# Download models (required!)
mkdir -p models
cd models
# Download model files from upscayl releases
wget https://github.com/upscayl/upscayl-ncnn/releases/download/v2.11.0/models.zip
unzip models.zip
cd ..
```

### 3. Start Services

#### Development Mode
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

#### Production Mode
```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### 4. Verify Deployment

```bash
# Check all services are running
docker-compose ps

# Check logs
docker-compose logs -f api

# Test API health
curl http://localhost:8080/actuator/health

# Access frontend
open http://localhost:3000
```

## 🏗️ Architecture

```
┌─────────────────┐
│   Frontend      │  Port 3000 (Nginx + React)
│   (Nginx/React) │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   API Server    │  Port 8080 (Spring Boot)
│  (Spring Boot)  │
└────┬───────┬────┘
     │       │
     ▼       ▼
┌─────────┐ ┌──────────┐
│ Database│ │ Upscayl  │
│(Postgres)│ │ Service  │
└─────────┘ └──────────┘
     │           │
     ▼           ▼
┌─────────┐ ┌──────────┐
│  Redis  │ │Prometheus│
│ (Cache) │ │(Metrics) │
└─────────┘ └──────────┘
```

## ⚙️ Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database
DB_PASSWORD=your_secure_password

# API
SPRING_PROFILES_ACTIVE=prod
API_PORT=8080

# Image Processing
UPSCAYL_DEFAULT_SCALE=2
IMAGE_PROCESSING_MAX_FILE_SIZE=50MB

# Security
JWT_SECRET=your_jwt_secret_here
CORS_ALLOWED_ORIGINS=https://yourdomain.com
```

### Docker Compose Files

- `docker-compose.yml` - Base configuration
- `docker-compose.dev.yml` - Development overrides (hot reload, dev tools)
- `docker-compose.prod.yml` - Production overrides (replicas, monitoring, backups)

## 🚢 Deployment

### Development Environment

```bash
# Start with hot reload
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up

# Access services:
# - Frontend: http://localhost:5173 (Vite dev server)
# - API: http://localhost:8080
# - Adminer: http://localhost:8081 (database UI)
# - MailHog: http://localhost:8025 (email testing)
```

### Production Environment

```bash
# Build and start
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Check status
docker-compose ps
docker-compose logs -f

# Scale API instances
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d --scale api=3

# Access services:
# - Frontend: http://localhost:3000
# - API: http://localhost:8080
# - Prometheus: http://localhost:9090
# - Grafana: http://localhost:3001
```

### GPU Support (for faster upscaling)

1. Install NVIDIA Container Toolkit:
```bash
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | sudo apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
  sudo tee /etc/apt/sources.list.d/nvidia-docker.list

sudo apt-get update
sudo apt-get install -y nvidia-container-toolkit
sudo systemctl restart docker
```

2. Uncomment GPU sections in `docker-compose.yml`:
```yaml
services:
  upscayl:
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

3. Restart services:
```bash
docker-compose up -d upscayl
```

## 📊 Monitoring

### Prometheus Metrics

Access Prometheus at `http://localhost:9090`

Available metrics:
- `http_server_requests_seconds_*` - API request metrics
- `jvm_memory_*` - JVM memory usage
- `system_cpu_usage` - CPU utilization
- `image_processing_*` - Custom image processing metrics

### Grafana Dashboards

Access Grafana at `http://localhost:3001` (default: admin/admin)

Pre-configured dashboards for:
- API Performance
- Database Metrics
- System Resources
- Image Processing Stats

### Logs

```bash
# View all logs
docker-compose logs -f

# View specific service
docker-compose logs -f api

# View with timestamps
docker-compose logs -f --timestamps api

# Last 100 lines
docker-compose logs --tail=100 api
```

## 🔧 Maintenance

### Backups

Automated daily backups are configured in production mode:

```bash
# Manual backup
docker-compose exec db pg_dump -U muttcues muttcues > backup_$(date +%Y%m%d).sql

# Restore backup
docker-compose exec -T db psql -U muttcues muttcues < backup_20260212.sql
```

### Updates

```bash
# Pull latest images
docker-compose pull

# Rebuild and restart
docker-compose build --no-cache
docker-compose up -d

# Clean old images
docker image prune -a
```

### Health Checks

```bash
# Check all service health
docker-compose ps

# API health
curl http://localhost:8080/actuator/health

# Database health
docker-compose exec db pg_isready -U muttcues

# Redis health
docker-compose exec redis redis-cli ping
```

## 🐛 Troubleshooting

### Common Issues

#### 1. Upscayl binary not found
```bash
# Check if binary exists
docker-compose exec upscayl ls -la /app/upscayl-ncnn/build/

# Rebuild upscayl image
docker-compose build --no-cache upscayl
```

#### 2. Out of memory errors
```bash
# Increase Docker memory limit (Docker Desktop)
# Settings > Resources > Memory > 8GB+

# Or modify compose file
services:
  api:
    deploy:
      resources:
        limits:
          memory: 4G
```

#### 3. Database connection refused
```bash
# Check database is running
docker-compose ps db

# Check logs
docker-compose logs db

# Restart database
docker-compose restart db
```

#### 4. Frontend can't connect to API
```bash
# Check API is accessible
curl http://localhost:8080/actuator/health

# Check CORS configuration in application.properties
# Verify nginx proxy configuration
```

#### 5. Models not found for upscaling
```bash
# Verify models are mounted
docker-compose exec upscayl ls -la /app/models/

# Re-download models
# See "Build Upscayl Binary" section
```

### Debug Commands

```bash
# Enter container shell
docker-compose exec api /bin/bash
docker-compose exec upscayl /bin/bash

# View container resources
docker stats

# Inspect container
docker inspect muttcues-api

# View network
docker network inspect muttcues_muttcues-network

# Clean everything and start fresh
docker-compose down -v
docker system prune -a
docker-compose up -d
```

## 📁 Directory Structure

```
MuttCUES/
├── MuttCUES-API/           # Spring Boot API source
├── MuttCUES-FE/            # React frontend source
├── Dockerfile.api          # API container definition
├── Dockerfile.frontend     # Frontend container definition
├── Dockerfile.upscayl      # Upscayl service definition
├── docker-compose.yml      # Base compose config
├── docker-compose.dev.yml  # Development overrides
├── docker-compose.prod.yml # Production overrides
├── nginx.conf              # Nginx configuration
├── prometheus.yml          # Monitoring config
├── init-db.sql            # Database schema
├── .env                    # Environment variables
├── .dockerignore          # Build exclusions
├── models/                 # AI models for upscaling
│   ├── realesrgan-x4plus/
│   └── realesrnet-x4plus/
└── backups/               # Database backups
```

## 🔐 Security Considerations

1. **Change default passwords** in `.env`
2. **Use secrets management** for production (Docker Secrets or Vault)
3. **Enable HTTPS** with Let's Encrypt
4. **Configure firewall** to expose only necessary ports
5. **Regular updates** of base images
6. **Monitor logs** for suspicious activity

## 📝 Additional Resources

- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [React Production Build](https://create-react-app.dev/docs/production-build/)
- [Docker Compose Best Practices](https://docs.docker.com/compose/compose-file/compose-file-v3/)
- [Upscayl Documentation](https://github.com/upscayl/upscayl-ncnn)

## 🤝 Support

For issues and questions:
- GitHub Issues: [MuttCUES-API](https://github.com/Mutt-Net/MuttCUES-API/issues)
- GitHub Issues: [MuttCUES-FE](https://github.com/Mutt-Net/MuttCUES-FE/issues)

## 📄 License

See LICENSE file in respective repositories.
