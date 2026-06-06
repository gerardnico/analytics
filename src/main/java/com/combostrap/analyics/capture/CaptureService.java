package com.combostrap.analyics.capture;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.combostrap.analyics.messaging.EventPublisher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application-scoped service backing the {@code /capture} endpoint.
 * <p>
 * It is a singleton because JAX-RS resources are instantiated per request/event
 * loop by RESTEasy and therefore cannot hold long-lived collaborators (the
 * RabbitMQ publisher, the rate limiter) themselves. The verticle wires it up at
 * startup via {@link #init}.
 * <p>
 * Flow for a request:
 * <ol>
 *   <li>parse + validate the PostHog-style payload (400 on bad input),</li>
 *   <li>apply the rate limit (429 when exceeded),</li>
 *   <li>enrich + hand the event off to RabbitMQ <em>without waiting</em> and
 *       return 200 immediately — fire-and-forget.</li>
 * </ol>
 */
public class CaptureService {

    private static final Logger LOGGER = Logger.getLogger(CaptureService.class.getName());

    private static volatile CaptureService INSTANCE;

    private final Vertx vertx;
    private final EventPublisher publisher;
    private final RateLimiter rateLimiter;

    private CaptureService(Vertx vertx, EventPublisher publisher, RateLimiter rateLimiter) {
        this.vertx = vertx;
        this.publisher = publisher;
        this.rateLimiter = rateLimiter;
    }

    public static void init(Vertx vertx, EventPublisher publisher, AnalyticsConfig config) {
        INSTANCE = new CaptureService(vertx, publisher, new RateLimiter(config));
    }

    public static CaptureService instance() {
        CaptureService local = INSTANCE;
        if (local == null) {
            throw new IllegalStateException("CaptureService not initialized");
        }
        return local;
    }

    /**
     * Outcome of a capture call, mapped to an HTTP status by the resource.
     */
    public record Result(int status, String body) {
    }

    /**
     * Handle one capture request.
     *
     * @param rawBody   the raw JSON request body (PostHog capture payload)
     * @param clientKey a stable identifier for the caller (e.g. client IP) used
     *                  for rate limiting when no api_key is present
     */
    public Result handle(String rawBody, String clientKey) {
        JsonObject payload;
        try {
            payload = new JsonObject(rawBody == null ? "" : rawBody);
        } catch (Exception e) {
            return new Result(400, "{\"error\":\"invalid json\"}");
        }

        String event = payload.getString("event");
        if (event == null || event.isBlank()) {
            return new Result(400, "{\"error\":\"missing 'event'\"}");
        }

        String apiKey = payload.getString("api_key");
        String rateKey = (apiKey != null && !apiKey.isBlank()) ? "key:" + apiKey : "ip:" + clientKey;
        if (!rateLimiter.tryAcquire(rateKey)) {
            return new Result(429, "{\"error\":\"rate limited\"}");
        }

        JsonObject message = toMessage(payload, event, apiKey);

        // Fire-and-forget: offload the publish and do NOT wait on it. The broker
        // write is the durability boundary; we answer 200 to the client right away.
        vertx.executeBlocking(() -> {
            publisher.publish(message);
            return null;
        }, false).onFailure(err ->
                LOGGER.log(Level.WARNING, "Failed to enqueue event " + message.getString("uuid"), err));

        return new Result(200, "{\"status\":1}");
    }

    /**
     * Maps the PostHog-style payload onto the canonical message put on the queue.
     * The data model is realm &gt; app &gt; (events, users): the api_key identifies
     * the app, the realm is carried in the properties (or defaults), and the user
     * is the distinct_id.
     */
    private JsonObject toMessage(JsonObject payload, String event, String apiKey) {
        JsonObject properties = payload.getJsonObject("properties", new JsonObject());

        String uuid = payload.getString("uuid");
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
        }

        String distinctId = payload.getString("distinct_id", properties.getString("distinct_id", "anonymous"));
        String appId = properties.getString("app_id", apiKey != null ? apiKey : "");
        String realmId = payload.getString("realm", properties.getString("realm_id", "default"));
        String timestamp = payload.getString("timestamp", Instant.now().toString());

        return new JsonObject()
                .put("uuid", uuid)
                .put("event", event)
                .put("api_key", apiKey)
                .put("realm_id", realmId)
                .put("app_id", appId)
                .put("distinct_id", distinctId)
                .put("timestamp", timestamp)
                .put("received_at", Instant.now().toString())
                .put("properties", properties);
    }
}
