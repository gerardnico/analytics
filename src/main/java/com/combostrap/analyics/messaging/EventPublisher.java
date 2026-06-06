package com.combostrap.analyics.messaging;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import io.vertx.core.json.JsonObject;

import java.nio.charset.StandardCharsets;

/**
 * Publishes capture events to the RabbitMQ events queue.
 * <p>
 * One {@link Connection} and a single publisher {@link Channel} are held for the
 * lifetime of the application. {@code basicPublish} writes to a local buffer and
 * returns quickly, which is what lets the HTTP layer answer {@code 200} early
 * (fire-and-forget). The channel is not thread-safe, so {@link #publish} is
 * synchronized.
 */
public class EventPublisher {

    private final AnalyticsConfig config;
    private Connection connection;
    private Channel channel;

    public EventPublisher(AnalyticsConfig config) {
        this.config = config;
    }

    public void start() throws Exception {
        this.connection = RabbitMQFactory.create(config).newConnection("analytics-publisher");
        this.channel = connection.createChannel();
        Topology.declare(channel, config);
    }

    /**
     * Publishes one event. The message is marked persistent so it survives a
     * broker restart while it waits in the queue.
     */
    public synchronized void publish(JsonObject event) throws Exception {
        AMQP.BasicProperties props = MessageProperties.PERSISTENT_TEXT_PLAIN;
        channel.basicPublish(
                "",                       // default exchange
                config.eventsQueue(),     // routing key == queue name
                props,
                event.encode().getBytes(StandardCharsets.UTF_8));
    }

    public void close() {
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
}
