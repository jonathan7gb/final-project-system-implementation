CREATE TABLE audit.logs (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_type   TEXT        NOT NULL,
    payload      JSONB       NOT NULL,
    user_id      UUID,
    occurred_at  TIMESTAMP   NOT NULL DEFAULT now(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_event_type  ON audit.logs (event_type);
CREATE INDEX idx_audit_logs_occurred_at ON audit.logs (occurred_at);
CREATE INDEX idx_audit_logs_user_id     ON audit.logs (user_id);
