CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    last_error TEXT
);

CREATE INDEX outbox_events_status_created_at_idx ON outbox_events (status, created_at, id);
CREATE INDEX outbox_events_aggregate_type_aggregate_id_idx ON outbox_events (aggregate_type, aggregate_id);
CREATE INDEX outbox_events_created_at_idx ON outbox_events (created_at);
