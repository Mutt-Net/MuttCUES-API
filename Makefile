# MuttCUES Makefile - Simplified Docker Commands

.PHONY: help build up down logs clean dev prod restart scale backup restore test

# Default target
.DEFAULT_GOAL := help

# Colors for output
BLUE := \033[0;34m
GREEN := \033[0;32m
RED := \033[0;31m
NC := \033[0m # No Color

## help: Display this help message
help:
	@echo "$(BLUE)MuttCUES Docker Management$(NC)"
	@echo ""
	@echo "Available commands:"
	@awk 'BEGIN {FS = ":.*##"; printf "\n"} /^[a-zA-Z_-]+:.*?##/ { printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2 } /^##@/ { printf "\n$(BLUE)%s$(NC)\n", substr($$0, 5) } ' $(MAKEFILE_LIST)

##@ Development

## dev: Start development environment with hot reload
dev:
	@echo "$(GREEN)Starting development environment...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d
	@echo "$(GREEN)Development environment started!$(NC)"
	@echo "Frontend: http://localhost:5173"
	@echo "API: http://localhost:8080"
	@echo "Adminer: http://localhost:8081"

## dev-build: Build and start development environment
dev-build:
	@echo "$(GREEN)Building development environment...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml build
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

## dev-logs: Follow development logs
dev-logs:
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

##@ Production

## prod: Start production environment
prod:
	@echo "$(GREEN)Starting production environment...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
	@echo "$(GREEN)Production environment started!$(NC)"
	@echo "Frontend: http://localhost:3000"
	@echo "API: http://localhost:8080"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3001"

## prod-build: Build and start production environment
prod-build:
	@echo "$(GREEN)Building production environment...$(NC)"
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml build --no-cache
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

## prod-logs: Follow production logs
prod-logs:
	docker-compose -f docker-compose.yml -f docker-compose.prod.yml logs -f

##@ General

## build: Build all services
build:
	@echo "$(GREEN)Building all services...$(NC)"
	docker-compose build

## up: Start all services (base configuration)
up:
	@echo "$(GREEN)Starting services...$(NC)"
	docker-compose up -d

## down: Stop all services
down:
	@echo "$(RED)Stopping all services...$(NC)"
	docker-compose down

## restart: Restart all services
restart: down up
	@echo "$(GREEN)Services restarted!$(NC)"

## logs: View logs from all services
logs:
	docker-compose logs -f

## ps: Show running containers
ps:
	@echo "$(BLUE)Running containers:$(NC)"
	docker-compose ps

##@ Maintenance

## clean: Remove all containers, networks, and volumes
clean:
	@echo "$(RED)Cleaning up...$(NC)"
	docker-compose down -v --remove-orphans
	docker system prune -f
	@echo "$(GREEN)Cleanup complete!$(NC)"

## clean-all: Deep clean including images
clean-all:
	@echo "$(RED)Deep cleaning...$(NC)"
	docker-compose down -v --remove-orphans --rmi all
	docker system prune -af --volumes
	@echo "$(GREEN)Deep cleanup complete!$(NC)"

## backup: Backup database
backup:
	@echo "$(GREEN)Creating database backup...$(NC)"
	@mkdir -p backups
	docker-compose exec -T db pg_dump -U muttcues muttcues > backups/backup_$$(date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)Backup created in backups/$(NC)"

## restore: Restore database from backup (use BACKUP=filename)
restore:
	@if [ -z "$(BACKUP)" ]; then \
		echo "$(RED)Error: Specify backup file with BACKUP=filename$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)Restoring database from $(BACKUP)...$(NC)"
	docker-compose exec -T db psql -U muttcues muttcues < $(BACKUP)
	@echo "$(GREEN)Database restored!$(NC)"

##@ Monitoring

## monitor: Start monitoring services (Prometheus & Grafana)
monitor:
	@echo "$(GREEN)Starting monitoring services...$(NC)"
	docker-compose --profile monitoring up -d prometheus grafana
	@echo "$(GREEN)Monitoring started!$(NC)"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3001"

## health: Check health of all services
health:
	@echo "$(BLUE)Checking service health...$(NC)"
	@docker-compose ps
	@echo ""
	@echo "API Health:"
	@curl -s http://localhost:8080/actuator/health | jq '.' || echo "API not available"
	@echo ""
	@echo "Database Health:"
	@docker-compose exec -T db pg_isready -U muttcues || echo "Database not available"
	@echo ""
	@echo "Redis Health:"
	@docker-compose exec -T redis redis-cli ping || echo "Redis not available"

##@ Testing

## test-api: Run API tests
test-api:
	@echo "$(GREEN)Running API tests...$(NC)"
	docker-compose exec api ./mvnw test

## test-upscale: Test upscaling with sample image
test-upscale:
	@echo "$(GREEN)Testing image upscaling...$(NC)"
	@if [ ! -f "test-image.png" ]; then \
		echo "$(RED)Error: test-image.png not found$(NC)"; \
		exit 1; \
	fi
	curl -X POST http://localhost:8080/api/image/upscale \
		-F "file=@test-image.png" \
		-F "scale=2"

##@ Scaling

## scale-api: Scale API instances (use REPLICAS=n)
scale-api:
	@if [ -z "$(REPLICAS)" ]; then \
		echo "$(RED)Error: Specify number of replicas with REPLICAS=n$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)Scaling API to $(REPLICAS) instances...$(NC)"
	docker-compose up -d --scale api=$(REPLICAS)

## scale-down: Scale down to single instances
scale-down:
	@echo "$(GREEN)Scaling down to single instances...$(NC)"
	docker-compose up -d --scale api=1 --scale frontend=1

##@ Utilities

## shell-api: Open shell in API container
shell-api:
	docker-compose exec api /bin/bash

## shell-db: Open psql shell in database
shell-db:
	docker-compose exec db psql -U muttcues muttcues

## shell-upscayl: Open shell in upscayl container
shell-upscayl:
	docker-compose exec upscayl /bin/bash

## models: Download AI models for upscaling
models:
	@echo "$(GREEN)Downloading AI models...$(NC)"
	@mkdir -p models
	@echo "Please download models from:"
	@echo "https://github.com/upscayl/upscayl-ncnn/releases"
	@echo "Extract to ./models/ directory"

## env: Create .env file from example
env:
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(GREEN).env file created! Please edit it with your configuration.$(NC)"; \
	else \
		echo "$(RED).env file already exists!$(NC)"; \
	fi

## init: Initialize project (create .env, download models)
init: env
	@echo "$(BLUE)Initializing MuttCUES...$(NC)"
	@echo "Next steps:"
	@echo "1. Edit .env file with your configuration"
	@echo "2. Download models (make models)"
	@echo "3. Start development (make dev) or production (make prod)"

##@ Information

## version: Show Docker and Docker Compose versions
version:
	@echo "$(BLUE)Docker Version:$(NC)"
	@docker --version
	@echo "$(BLUE)Docker Compose Version:$(NC)"
	@docker-compose --version

## config: Validate and view compose configuration
config:
	@echo "$(BLUE)Docker Compose Configuration:$(NC)"
	docker-compose config

## stats: Show container resource usage
stats:
	docker stats --no-stream
