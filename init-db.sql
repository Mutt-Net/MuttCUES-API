-- MuttCUES Database Initialization Script

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create schemas
CREATE SCHEMA IF NOT EXISTS muttcues;
SET search_path TO muttcues, public;

-- Files table
CREATE TABLE IF NOT EXISTS files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id VARCHAR(255) UNIQUE NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(255),
    status VARCHAR(50) DEFAULT 'uploaded',
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Processed files table
CREATE TABLE IF NOT EXISTS processed_files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_id UUID REFERENCES files(id) ON DELETE CASCADE,
    processed_file_id VARCHAR(255) UNIQUE NOT NULL,
    processed_name VARCHAR(500) NOT NULL,
    processing_type VARCHAR(50) NOT NULL, -- upscale, dds_to_png, image_to_dds
    processing_params JSONB,
    file_size BIGINT,
    content_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'processing', -- processing, completed, failed
    error_message TEXT,
    processing_started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    processing_completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Processing jobs table (for async processing)
CREATE TABLE IF NOT EXISTS processing_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id VARCHAR(255) UNIQUE NOT NULL,
    file_id UUID REFERENCES files(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL,
    parameters JSONB,
    status VARCHAR(50) DEFAULT 'queued', -- queued, processing, completed, failed, cancelled
    priority INTEGER DEFAULT 5,
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    error_message TEXT,
    queued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Users table (for future authentication)
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'user',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP WITH TIME ZONE
);

-- API keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    key_hash VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255),
    permissions JSONB,
    is_active BOOLEAN DEFAULT true,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Usage statistics table
CREATE TABLE IF NOT EXISTS usage_statistics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    operation_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    processing_duration_ms BIGINT,
    success BOOLEAN,
    error_type VARCHAR(100),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_files_file_id ON files(file_id);
CREATE INDEX IF NOT EXISTS idx_files_upload_date ON files(upload_date);
CREATE INDEX IF NOT EXISTS idx_files_status ON files(status);

CREATE INDEX IF NOT EXISTS idx_processed_files_file_id ON processed_files(file_id);
CREATE INDEX IF NOT EXISTS idx_processed_files_processed_file_id ON processed_files(processed_file_id);
CREATE INDEX IF NOT EXISTS idx_processed_files_status ON processed_files(status);
CREATE INDEX IF NOT EXISTS idx_processed_files_type ON processed_files(processing_type);

CREATE INDEX IF NOT EXISTS idx_processing_jobs_job_id ON processing_jobs(job_id);
CREATE INDEX IF NOT EXISTS idx_processing_jobs_status ON processing_jobs(status);
CREATE INDEX IF NOT EXISTS idx_processing_jobs_queued_at ON processing_jobs(queued_at);
CREATE INDEX IF NOT EXISTS idx_processing_jobs_priority ON processing_jobs(priority);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_api_keys_user_id ON api_keys(user_id);

CREATE INDEX IF NOT EXISTS idx_usage_statistics_user_id ON usage_statistics(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_statistics_timestamp ON usage_statistics(timestamp);
CREATE INDEX IF NOT EXISTS idx_usage_statistics_operation_type ON usage_statistics(operation_type);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_files_updated_at BEFORE UPDATE ON files
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_processed_files_updated_at BEFORE UPDATE ON processed_files
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_processing_jobs_updated_at BEFORE UPDATE ON processing_jobs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Views
CREATE OR REPLACE VIEW file_processing_summary AS
SELECT 
    f.file_id,
    f.original_name,
    f.upload_date,
    COUNT(pf.id) as processing_count,
    MAX(pf.processing_completed_at) as last_processed,
    json_agg(
        json_build_object(
            'type', pf.processing_type,
            'status', pf.status,
            'completed_at', pf.processing_completed_at
        )
    ) as processing_history
FROM files f
LEFT JOIN processed_files pf ON f.id = pf.file_id
GROUP BY f.id, f.file_id, f.original_name, f.upload_date;

CREATE OR REPLACE VIEW processing_queue_status AS
SELECT 
    status,
    job_type,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - queued_at))) as avg_wait_time_seconds,
    MAX(queued_at) as oldest_job_time
FROM processing_jobs
WHERE status IN ('queued', 'processing')
GROUP BY status, job_type;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA muttcues TO muttcues;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA muttcues TO muttcues;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA muttcues TO muttcues;

-- Insert sample data (optional, for testing)
-- INSERT INTO users (username, email, password_hash, role) 
-- VALUES ('admin', 'admin@muttcues.local', '$2a$10$...', 'admin');

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'MuttCUES database initialized successfully';
END
$$;
