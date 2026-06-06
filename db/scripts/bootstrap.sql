-- // create changelog
-- MyBatis Migrations bookkeeping table, in ClickHouse syntax.
-- IF NOT EXISTS keeps it idempotent whether or not `migrate bootstrap` ran first.

CREATE TABLE IF NOT EXISTS CHANGELOG
(
    ID
    Int64,
    APPLIED_AT
    String,
    DESCRIPTION
    String
)
    ENGINE = MergeTree
    ORDER BY ID;

-- //@UNDO

DROP TABLE IF EXISTS CHANGELOG;