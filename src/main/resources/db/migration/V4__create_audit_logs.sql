CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX audit_logs_entity_type_idx ON audit_logs (entity_type);
CREATE INDEX audit_logs_entity_type_entity_id_idx ON audit_logs (entity_type, entity_id);
CREATE INDEX audit_logs_created_at_idx ON audit_logs (created_at);
