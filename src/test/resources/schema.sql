-- Schema for H2 tests
CREATE TABLE IF NOT EXISTS files (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(255) UNIQUE NOT NULL,
    original_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),
    upload_date TIMESTAMP WITH TIME ZONE NOT NULL,
    uploaded_by VARCHAR(255),
    status VARCHAR(50) DEFAULT 'uploaded' NOT NULL,
    metadata VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS processed_files (
    id VARCHAR(36) PRIMARY KEY,
    file_id VARCHAR(36) NOT NULL,
    processed_file_id VARCHAR(255) UNIQUE NOT NULL,
    processed_name VARCHAR(500) NOT NULL,
    processing_type VARCHAR(50) NOT NULL,
    processing_params VARCHAR(255),
    file_size BIGINT,
    content_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'processing' NOT NULL,
    error_message VARCHAR(255),
    processing_started_at TIMESTAMP WITH TIME ZONE,
    processing_completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (file_id) REFERENCES files(id)
);
