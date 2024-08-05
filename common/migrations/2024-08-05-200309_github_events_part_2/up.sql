-- Your SQL goes here
ALTER TABLE "events" DROP CONSTRAINT check_event_type;
ALTER TABLE "events" ADD CONSTRAINT check_event_type CHECK (
    ("target_alias_id" IS NOT NULL AND ("event_type" != 'custom' OR "event_type" != 'github'))
    OR ("custom_alias_id" IS NOT NULL AND ("event_type" = 'custom' OR "event_type" = 'github'))
);