package com.combostrap.analyics.messaging;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Map;

/**
 * Declares the RabbitMQ topology used by the ingestion pipeline.
 * <p>
 * The declarations are idempotent, so both the publisher (HTTP side) and the
 * worker (consumer side) can declare them on startup without coordination.
 * <p>
 * The main queue is configured with a dead-letter exchange. A message that the
 * worker definitively rejects (after exhausting its retries) is routed there and
 * lands in the dead-letter queue for later inspection/replay, instead of being
 * silently dropped or looping forever.
 */
public final class Topology {

    private Topology() {
    }

    public static void declare(Channel channel, AnalyticsConfig config) throws IOException {
        // Dead-letter side
        channel.exchangeDeclare(config.deadLetterExchange(), "fanout", true);
        channel.queueDeclare(config.deadLetterQueue(), true, false, false, null);
        channel.queueBind(config.deadLetterQueue(), config.deadLetterExchange(), "");

        // Main events queue, dead-lettering rejected messages to the DLX
        Map<String, Object> args = Map.of("x-dead-letter-exchange", config.deadLetterExchange());
        channel.queueDeclare(config.eventsQueue(), true, false, false, args);
    }
}
