package com.combostrap.analyics;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.combostrap.analyics.messaging.EventPublisher;
import com.combostrap.analyics.messaging.EventWorkerVerticle;
import com.combostrap.analyics.migration.Migrations;
import com.combostrap.analyics.store.ClickHouseEventStore;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end ingestion test:
 * <ol>
 *   <li>spin up ClickHouse and RabbitMQ with Testcontainers,</li>
 *   <li>run the MyBatis migrations to create the events table,</li>
 *   <li>deploy the worker and publish N random events through the real publisher,</li>
 *   <li>assert that exactly N rows land in the ClickHouse events table.</li>
 * </ol>
 */
@Testcontainers
class CapturePipelineIT {

    private static final int EVENT_COUNT = 50;

    @Container
    private final ClickHouseContainer clickhouse =
            new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server:24.8"));

    @Container
    private final RabbitMQContainer rabbitmq =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (vertx != null) {
            vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);
        }
    }

    @Test
    void ingestsAllPublishedEvents() throws Exception {
        AnalyticsConfig config = testConfig();

        // 1. schema
        Migrations.runUp(config);

        // 2. worker draining the queue into ClickHouse
        vertx.deployVerticle(new EventWorkerVerticle(config))
                .toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS);

        // 3. publish N random events through the real publisher
        EventPublisher publisher = new EventPublisher(config);
        publisher.start();
        Random random = new Random(42);
        for (int i = 0; i < EVENT_COUNT; i++) {
            publisher.publish(randomEvent(random, i));
        }
        publisher.close();

        // 4. wait until all events are stored, then assert the count
        ClickHouseEventStore store = new ClickHouseEventStore(config);
        store.init();
        long count = awaitCount(store, EVENT_COUNT, 60);
        assertEquals(EVENT_COUNT, count, "all published events should be stored in ClickHouse");
    }

    private AnalyticsConfig testConfig() {
        return new AnalyticsConfig(
                clickhouse.getJdbcUrl(),
                clickhouse.getUsername(),
                clickhouse.getPassword(),
                rabbitmq.getHost(),
                rabbitmq.getAmqpPort(),
                rabbitmq.getAdminUsername(),
                rabbitmq.getAdminPassword(),
                "/",
                "analytics.events",
                "analytics.events.dlx",
                "analytics.events.dlq",
                50,   // prefetch
                3,    // max retries
                500L, // retry backoff
                1000L, 1000L,
                new File("db/scripts").getAbsolutePath(),
                AnalyticsConfig.CLICKHOUSE_DRIVER);
    }

    private JsonObject randomEvent(Random random, int index) {
        String[] eventTypes = {"pageview", "click", "signup", "purchase"};
        return new JsonObject()
                .put("uuid", java.util.UUID.randomUUID().toString())
                .put("event", eventTypes[random.nextInt(eventTypes.length)])
                .put("api_key", "app-" + random.nextInt(3))
                .put("realm_id", "realm-" + random.nextInt(2))
                .put("app_id", "app-" + random.nextInt(3))
                .put("distinct_id", "user-" + random.nextInt(10))
                .put("timestamp", Instant.now().toString())
                .put("received_at", Instant.now().toString())
                .put("properties", new JsonObject().put("index", index));
    }

    private long awaitCount(ClickHouseEventStore store, int expected, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        long count = 0;
        while (System.nanoTime() < deadline) {
            count = store.count();
            if (count >= expected) {
                return count;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        return count;
    }
}
