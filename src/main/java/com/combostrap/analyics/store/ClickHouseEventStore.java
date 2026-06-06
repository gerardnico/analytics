package com.combostrap.analyics.store;

import com.combostrap.analyics.config.AnalyticsConfig;
import io.vertx.core.json.JsonObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Writes capture events into the single ClickHouse {@code events} table.
 * <p>
 * A fresh JDBC connection is opened per insert. This keeps the store trivially
 * thread-safe and side-steps the fact that a ClickHouse JDBC {@code Connection}
 * is not meant to be shared across threads. For higher throughput this is the
 * natural place to introduce a connection pool and batched inserts.
 */
public class ClickHouseEventStore {

    private static final String INSERT_SQL =
            "INSERT INTO events " +
                    "(uuid, event, realm_id, app_id, distinct_id, timestamp, received_at, properties) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final AnalyticsConfig config;

    public ClickHouseEventStore(AnalyticsConfig config) {
        this.config = config;
    }

    /**
     * Ensures the JDBC driver is registered. With JDBC 4 this is automatic, but
     * we force it so a misconfigured classpath fails loudly and early.
     */
    public void init() {
        try {
            Class.forName(config.clickhouseDriver());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ClickHouse JDBC driver not on classpath", e);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                config.clickhouseUrl(), config.clickhouseUser(), config.clickhousePassword());
    }

    public void insert(JsonObject event) throws Exception {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {

            statement.setObject(1, parseUuid(event.getString("uuid")));
            statement.setString(2, event.getString("event", ""));
            statement.setString(3, event.getString("realm_id", "default"));
            statement.setString(4, event.getString("app_id", ""));
            statement.setString(5, event.getString("distinct_id", "anonymous"));
            statement.setTimestamp(6, toTimestamp(event.getString("timestamp")));
            statement.setTimestamp(7, toTimestamp(event.getString("received_at")));
            statement.setString(8, propertiesAsString(event));

            statement.execute();
        }
    }

    /**
     * Counts the rows in the events table. Used by the integration test.
     */
    public long count() throws Exception {
        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count() FROM events")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null ? UUID.randomUUID() : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static Timestamp toTimestamp(String iso) {
        if (iso == null || iso.isBlank()) {
            return Timestamp.from(Instant.now());
        }
        try {
            // Handles offsets like 2026-06-06T10:15:30Z / +02:00
            return Timestamp.from(OffsetDateTime.parse(iso).toInstant());
        } catch (Exception e) {
            try {
                return Timestamp.from(Instant.parse(iso));
            } catch (Exception ignored) {
                return Timestamp.from(Instant.now());
            }
        }
    }

    private static String propertiesAsString(JsonObject event) {
        JsonObject properties = event.getJsonObject("properties");
        return properties == null ? "{}" : properties.encode();
    }
}
