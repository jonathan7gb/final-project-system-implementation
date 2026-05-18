CREATE TABLE event_publication (
  id                UUID        PRIMARY KEY,
  listener_id       TEXT        NOT NULL,
  event_type        TEXT        NOT NULL,
  serialized_event  TEXT        NOT NULL,
  publication_date  TIMESTAMP   NOT NULL,
  completion_date   TIMESTAMP
);

CREATE INDEX idx_event_publication_completion_date
  ON event_publication (completion_date);
