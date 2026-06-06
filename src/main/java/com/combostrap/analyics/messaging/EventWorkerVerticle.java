package com.combostrap.analyics.messaging;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.combostrap.analyics.store.ClickHouseEventStore;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Consumes events from the RabbitMQ queue and stores each one in the single
 * ClickHouse {@code events} table.
 * <p>
 * Error handling (manual ack + dead-letter + bounded retry):
 * <ul>
 *   <li>The consumer runs with manual acknowledgement. A message is acked only
 *       once it is durably written to ClickHouse, so a crash mid-insert leaves
 *       the message in the queue for redelivery.</li>
 *   <li>On a failed insert the message is retried up to {@code maxRetries} times
 *       (tracked in-memory by event uuid, with a small backoff before requeue).</li>
 *   <li>Once retries are exhausted the message is rejected without requeue, which
 *       dead-letters it to the configured DLX/DLQ for inspection instead of
 *       looping forever or being lost.</li>
 * </ul>
 */
public class EventWorkerVerticle extends AbstractVerticle {

    private static final Logger LOGGER = Logger.getLogger(EventWorkerVerticle.class.getName());

    private final AnalyticsConfig config;
    private final ClickHouseEventStore store;
    private final ConcurrentHashMap<String, Integer> attempts = new ConcurrentHashMap<>();

    private Connection connection;
    private Channel channel;

    public EventWorkerVerticle(AnalyticsConfig config) {
        this.config = config;
        this.store = new ClickHouseEventStore(config);
    }

    @Override
    public void start(Promise<Void> startPromise) {
        vertx.executeBlocking(() -> {
            store.init();
            connection = RabbitMQFactory.create(config).newConnection("analytics-worker");
            channel = connection.createChannel();
            Topology.declare(channel, config);
            channel.basicQos(config.workerPrefetch());
            channel.basicConsume(config.eventsQueue(), false, new EventConsumer(channel));
            return null;
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                LOGGER.info("Event worker consuming from queue '" + config.eventsQueue() + "'");
                startPromise.complete();
            } else {
                startPromise.fail(ar.cause());
            }
        });
    }

    @Override
    public void stop() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
        } catch (Exception ignored) {
            // best effort
        }
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception ignored) {
            // best effort
        }
    }

    private final class EventConsumer extends DefaultConsumer {

        EventConsumer(Channel channel) {
            super(channel);
        }

        @Override
        public void handleDelivery(String consumerTag, Envelope envelope,
                                   AMQP.BasicProperties properties, byte[] body) {
            long tag = envelope.getDeliveryTag();
            JsonObject event;
            try {
                event = new JsonObject(new String(body, java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception parseError) {
                // Unparseable payload can never succeed: dead-letter it immediately.
                LOGGER.log(Level.WARNING, "Dropping unparseable message to DLQ", parseError);
                nack(tag, false);
                return;
            }

            String uuid = event.getString("uuid", String.valueOf(tag));
            try {
                store.insert(event);
                attempts.remove(uuid);
                channel.basicAck(tag, false);
            } catch (Exception insertError) {
                handleFailure(tag, uuid, insertError);
            }
        }

        private void handleFailure(long tag, String uuid, Exception error) {
            int count = attempts.merge(uuid, 1, Integer::sum);
            if (count <= config.maxRetries()) {
                LOGGER.log(Level.WARNING,
                        "Insert failed (attempt " + count + "/" + config.maxRetries()
                                + ") for event " + uuid + ", requeueing", error);
                // Backoff before requeue to avoid a hot retry loop.
                vertx.setTimer(config.retryBackoffMs(), t -> nack(tag, true));
            } else {
                LOGGER.log(Level.SEVERE,
                        "Insert permanently failed for event " + uuid + ", dead-lettering", error);
                attempts.remove(uuid);
                nack(tag, false);
            }
        }

        private void nack(long tag, boolean requeue) {
            try {
                channel.basicNack(tag, false, requeue);
            } catch (Exception nackError) {
                LOGGER.log(Level.SEVERE, "Failed to nack delivery " + tag, nackError);
            }
        }
    }
}
