CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          actor_email VARCHAR(150),
                                          actor_name VARCHAR(150),
                                          actor_roles VARCHAR(255),
                                          module VARCHAR(50) NOT NULL,
                                          action VARCHAR(100) NOT NULL,
                                          description TEXT,
                                          target_id VARCHAR(100),
                                          http_method VARCHAR(10),
                                          endpoint VARCHAR(255),
                                          ip_address VARCHAR(50),
                                          status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
                                          error_message TEXT,
                                          execution_time_ms BIGINT,
                                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_logs_module ON audit_logs (module);
CREATE INDEX idx_audit_logs_actor_email ON audit_logs (actor_email);