-- // create events table
--
-- Single wide events table, following the PostHog / Mixpanel "one big events
-- table" convention: every captured event is one immutable row, with the
-- variable bits kept in a raw JSON `properties` column. This avoids per-event-type
-- tables and lets new event shapes land without a migration.
--
-- Data model: a realm has one or more apps and one or more users; an event is
-- produced by an app (which belongs to a realm) on behalf of a user
-- (distinct_id).
--
-- Sort key choice. The primary ORDER BY drives the realm / app / event-type
-- access paths as key prefixes:
--   realm                -> (realm_id)
--   realm + app          -> (realm_id, app_id)
--   realm + app + event  -> (realm_id, app_id, event)
-- The two normal PROJECTIONS reorder the same data to make the remaining
-- "last event" lookups efficient without a full scan:
--   by_user  -> ORDER BY (distinct_id, timestamp)
--   by_event -> ORDER BY (event, timestamp)
--
-- "Last event by timestamp" example queries:
--   -- last event of a given type
--   SELECT * FROM events WHERE event = 'pageview'        ORDER BY timestamp DESC LIMIT 1;
--   -- last event for a user
--   SELECT * FROM events WHERE distinct_id = 'user-123'  ORDER BY timestamp DESC LIMIT 1;
--   -- last event for an app
--   SELECT * FROM events WHERE realm_id = 'r1' AND app_id = 'a1' ORDER BY timestamp DESC LIMIT 1;
--   -- last event for a realm
--   SELECT * FROM events WHERE realm_id = 'r1'           ORDER BY timestamp DESC LIMIT 1;

CREATE TABLE IF NOT EXISTS events
(
    uuid        UUID,
    event       VARCHAR,
    properties  VARCHAR CODEC (ZSTD(3)),
    timestamp   DateTime64(6, 'UTC'),
    created_at  DateTime64(6, 'UTC'),
    received_at DateTime64(3, 'UTC') DEFAULT now64(3, 'UTC'),
    realm_id    Int64,
    app_id      Int64,
    -- the device id
    distinct_id String,
    person_id   UUID,

    PROJECTION by_user
        (
        SELECT *
        ORDER BY (distinct_id, timestamp)
        ),
    PROJECTION by_event
        (
        SELECT *
        ORDER BY (event, timestamp)
        )
)
    ENGINE = MergeTree
        PARTITION BY toYYYYMM(timestamp)
        ORDER BY (realm_id, app_id, event, distinct_id, timestamp);

-- //@UNDO

DROP TABLE IF EXISTS events;
