package com.combostrap.analyics.messaging;

import com.combostrap.analyics.config.AnalyticsConfig;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Builds a {@link ConnectionFactory} from the {@link AnalyticsConfig}.
 */
public final class RabbitMQFactory {

    private RabbitMQFactory() {
    }

    public static ConnectionFactory create(AnalyticsConfig config) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.rabbitHost());
        factory.setPort(config.rabbitPort());
        factory.setUsername(config.rabbitUser());
        factory.setPassword(config.rabbitPassword());
        factory.setVirtualHost(config.rabbitVirtualHost());
        // Recover automatically from connection drops
        factory.setAutomaticRecoveryEnabled(true);
        return factory;
    }
}
