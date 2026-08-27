ALTER TABLE notifications ADD COLUMN source_event_id VARCHAR(200);
UPDATE notifications SET source_event_id = id::text WHERE source_event_id IS NULL;
ALTER TABLE notifications ALTER COLUMN source_event_id SET NOT NULL;
ALTER TABLE notifications ADD CONSTRAINT notifications_source_event_unique UNIQUE (source_event_id);
