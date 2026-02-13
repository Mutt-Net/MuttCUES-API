# MuttCUES Docker Deployment - Complete Package

## 📦 What's Included

This package contains everything needed to deploy MuttCUES with Docker:

### Docker Configuration Files
1. **Dockerfile.api** - Multi-stage build for Spring Boot API
2. **Dockerfile.frontend** - React frontend with Nginx
3. **Dockerfile.upscayl** - Upscayl-NCNN image processing service
4. **docker-compose.yml** - Base orchestration
5. **docker-compose.dev.yml** - Development environment
6. **docker-compose.prod.yml** - Production environment with monitoring
7. **.dockerignore** - Build optimization
8. **nginx.conf** - Frontend web server configuration

### Configuration Files
9. **.env.example** - Environment variables template
10. **prometheus.yml** - Metrics collection
11. **init-db.sql** - Database schema initialization

### Automation
12. **Makefile** - Simplified commands for all operations

### Documentation
13. **DOCKER_README.md** - Comprehensive deployment guide
14. **progress.jsonl** - Development progress tracking

## 🚀 Quick Start Commands

### Initialize Project
```bash
make init              # Create .env and show setup steps
make env               # Create .env file
make models            # Instructions for downloading AI models
```

### Development
```bash
make dev               # Start dev environment with hot reload
make dev-logs          # View development logs
make shell-api         # Open API container shell
```

### Production
```bash
make prod              # Start production environment
make prod-build        # Rebuild and start production
make scale-api REPLICAS=3  # Scale API to 3 instances
```

### Monitoring
```bash
make monitor           # Start Prometheus & Grafana
make health            # Check all service health
make stats             # Show resource usage
```

### Maintenance
```bash
make backup            # Backup database
make restore BACKUP=file.sql  # Restore database
make clean             # Remove containers and volumes
```

## 📊 Service Ports

| Service | Development | Production |
|---------|-------------|------------|
| Frontend | 5173 | 3000 |
| API | 8080 | 8080 |
| Database | 5432 | - |
| Redis | 6379 | - |
| Prometheus | - | 9090 |
| Grafana | - | 3001 |
| Adminer | 8081 | - |

## 🏗️ Architecture Overview

```
Internet
    │
    ▼
┌─────────────┐
│   Nginx     │  (Frontend + API Proxy)
│  (Port 80)  │
└──────┬──────┘
       │
       ├──────────────┐
       │              │
       ▼              ▼
┌──────────┐   ┌──────────┐
│ Frontend │   │   API    │
│  React   │   │  Spring  │
└──────────┘   └────┬─────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
        ▼           ▼           ▼
  ┌─────────┐ ┌─────────┐ ┌─────────┐
  │Postgres │ │ Upscayl │ │  Redis  │
  └─────────┘ └─────────┘ └─────────┘
        │           │
        ▼           ▼
  ┌──────────────────────┐
  │  Prometheus/Grafana  │
  └──────────────────────┘
```

## 📋 Pre-Deployment Checklist

### 1. System Requirements
- [ ] Docker 24.0+ installed
- [ ] Docker Compose 2.20+ installed
- [ ] 8GB+ RAM available
- [ ] 20GB+ disk space free
- [ ] Ports 80, 443, 8080 available

### 2. Project Setup
- [ ] Clone MuttCUES-API repository
- [ ] Clone MuttCUES-FE repository
- [ ] Copy all Docker files to project root
- [ ] Create .env file from .env.example
- [ ] Update .env with secure passwords

### 3. Upscayl Setup
- [ ] Download upscayl-ncnn models
- [ ] Place models in ./models/ directory
- [ ] Verify models: ls -la models/

### 4. Security
- [ ] Change DB_PASSWORD in .env
- [ ] Set JWT_SECRET to random string
- [ ] Configure CORS_ALLOWED_ORIGINS
- [ ] Set GRAFANA_PASSWORD

### 5. Optional: GPU Support
- [ ] Install NVIDIA Container Toolkit
- [ ] Uncomment GPU sections in docker-compose.yml
- [ ] Test GPU: docker run --rm --gpus all nvidia/cuda:11.0-base nvidia-smi

## 🔧 Configuration Tips

### Database Tuning
For production, adjust PostgreSQL settings in docker-compose.prod.yml:
- `max_connections` - Based on expected load
- `shared_buffers` - 25% of available RAM
- `effective_cache_size` - 50% of available RAM

### API Performance
Adjust JVM settings in .env:
```bash
JAVA_OPTS=-Xmx4g -Xms1g -XX:+UseG1GC
```

### File Upload Limits
In application.properties or .env:
```bash
IMAGE_PROCESSING_MAX_FILE_SIZE=50MB
```

### Concurrent Processing
Control how many images can be processed simultaneously:
```bash
MAX_CONCURRENT_PROCESSING=4
```

## 🐛 Common Issues and Solutions

### Issue: Upscayl binary not found
**Solution:**
```bash
docker-compose build --no-cache upscayl
docker-compose up -d upscayl
docker-compose exec upscayl ls -la /app/upscayl-ncnn/build/
```

### Issue: Out of memory during build
**Solution:**
Increase Docker memory limit in Docker Desktop settings or:
```bash
docker-compose build --memory=8g
```

### Issue: Database connection failed
**Solution:**
```bash
docker-compose restart db
docker-compose logs db
```

### Issue: Models not loading
**Solution:**
```bash
# Verify models exist
ls -la models/

# Check mount in container
docker-compose exec upscayl ls -la /app/models/
```

## 📈 Scaling Guide

### Horizontal Scaling (Multiple Instances)
```bash
# Scale API to 3 instances
make scale-api REPLICAS=3

# Or with docker-compose
docker-compose up -d --scale api=3
```

### Vertical Scaling (More Resources)
Edit docker-compose.prod.yml:
```yaml
services:
  api:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
```

### Load Balancing
For production, add a reverse proxy (Nginx/Traefik) in front of API instances.

## 🔐 Security Best Practices

1. **Use Docker Secrets** for production:
```yaml
secrets:
  db_password:
    external: true
```

2. **Enable HTTPS** with Let's Encrypt:
```bash
# Add Certbot container to docker-compose.yml
```

3. **Network Isolation**:
```yaml
networks:
  frontend:
  backend:
    internal: true
```

4. **Regular Updates**:
```bash
# Weekly updates
docker-compose pull
docker-compose up -d
```

5. **Monitoring & Alerts**:
- Set up Grafana alerts
- Monitor disk space
- Track error rates

## 📚 Additional Resources

### Documentation
- [INTEGRATION_PLAN.md](INTEGRATION_PLAN.md) - Architecture and design
- [QUICK_START.md](QUICK_START.md) - Implementation code
- [DOCKER_README.md](DOCKER_README.md) - Detailed deployment guide

### External Links
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker/)
- [Upscayl Documentation](https://github.com/upscayl/upscayl-ncnn)

## 🎯 Next Steps

1. **Review Configuration**
   - Read DOCKER_README.md thoroughly
   - Understand each service's role
   - Plan resource allocation

2. **Setup Development**
   ```bash
   make init
   make dev
   make test-api
   ```

3. **Test Locally**
   - Upload test images
   - Try upscaling
   - Check processing times
   - Monitor resource usage

4. **Deploy to Production**
   ```bash
   make prod-build
   make monitor
   make backup
   ```

5. **Monitor & Optimize**
   - Watch Grafana dashboards
   - Review logs regularly
   - Optimize based on usage patterns

## 💡 Tips for Success

1. **Start with Development** - Don't go straight to production
2. **Test with Small Images** - Verify everything works first
3. **Monitor Resource Usage** - Use `docker stats` frequently
4. **Backup Regularly** - Schedule daily database backups
5. **Keep Logs** - They're invaluable for debugging
6. **Update Models** - New upscaling models improve quality
7. **Scale Gradually** - Add resources as needed
8. **Document Changes** - Keep track of configuration tweaks

## 🤝 Support & Contributing

### Getting Help
- Check DOCKER_README.md troubleshooting section
- Review logs: `make logs`
- Test health: `make health`

### Reporting Issues
Include:
- Docker version
- Docker Compose version
- Error messages
- Relevant logs
- Steps to reproduce

### Contributing
1. Test changes in development
2. Update documentation
3. Follow existing patterns
4. Add comments for complex logic

## 📄 License

See LICENSE files in respective repositories.

---

**Built with ❤️ for the MuttCUES project**

Last Updated: 2026-02-12
