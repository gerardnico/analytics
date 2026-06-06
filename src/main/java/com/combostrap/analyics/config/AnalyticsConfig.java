package com.combostrap.analyics.config;

/**
 * Runtime configuration resolved from environment variables (with sensible
 * localhost defaults that match the {@code docker-compose.yml} stack).
 * <p>
 * Kept as an immutable record so it can be built once at startup from the
 * environment, or constructed directly with explicit values from the test
 * harness (Testcontainers).
 */
public record AnalyticsConfig(
        // ClickHouse
        String clickhouseUrl,
        String clickhouseUser,
        String clickhousePassword,
        // RabbitMQ
        String rabbitHost,
        int rabbitPort,
        String rabbitUser,
        String rabbitPassword,
        String rabbitVirtualHost,
        // Messaging topology
        String eventsQueue,
        String deadLetterExchange,
        String deadLetterQueue,
        // Worker
        int workerPrefetch,
        int maxRetries,
        long retryBackoffMs,
        // Rate limiting (token bucket, per api_key / client ip)
        long rateLimitCapacity,
        long rateLimitRefillPerMinute,
        // Migration
        String migrationScriptsDir,
        String clickhouseDriver) {

    public static final String CLICKHOUSE_DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";

    public static AnalyticsConfig fromEnv() {
        return new AnalyticsConfig(
                env("CLICKHOUSE_URL", "jdbc:clickhouse://localhost:8123/analytics"),
                env("CLICKHOUSE_USER", "analytics"),
                env("CLICKHOUSE_PASSWORD", "analytics"),
                env("RABBITMQ_HOST", "localhost"),
                envInt("RABBITMQ_PORT", 5672),
                env("RABBITMQ_USER", "analytics"),
                env("RABBITMQ_PASSWORD", "analytics"),
                env("RABBITMQ_VHOST", "/"),
                env("EVENTS_QUEUE", "analytics.events"),
                env("EVENTS_DLX", "analytics.events.dlx"),
                env("EVENTS_DLQ", "analytics.events.dlq"),
                envInt("WORKER_PREFETCH", 50),
                envInt("WORKER_MAX_RETRIES", 3),
                envLong("WORKER_RETRY_BACKOFF_MS", 1000L),
                envLong("RATE_LIMIT_CAPACITY", 100L),
                envLong("RATE_LIMIT_REFILL_PER_MINUTE", 100L),
                env("DB_SCRIPTS_DIR", "db/scripts"),
                CLICKHOUSE_DRIVER);
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private static int envInt(String key, int defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : Integer.parseInt(value.trim());
    }

    private static long envLong(String key, long defaultValue) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? defaultValue : Long.parseLong(value.trim());
    }
}
