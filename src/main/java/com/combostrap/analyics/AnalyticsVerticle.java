package com.combostrap.analyics;

import com.combostrap.analyics.capture.CaptureService;
import com.combostrap.analyics.config.AnalyticsConfig;
import com.combostrap.analyics.messaging.EventPublisher;
import com.combostrap.analyics.messaging.EventWorkerVerticle;
import com.combostrap.analyics.resources.CaptureResource;
import com.combostrap.analyics.resources.OpenApiResource;
import com.combostrap.analyics.resources.PingResource;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import org.jboss.resteasy.plugins.server.vertx.VertxRegistry;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;

import java.util.Arrays;
import java.util.logging.Logger;


/**
 * Vert.x Verticle that configures the HTTP server with RESTEasy.
 * <p>
 * On startup it also wires the ingestion pipeline: it connects the RabbitMQ
 * publisher used by the {@code /capture} endpoint, initialises the
 * {@link CaptureService} singleton, and deploys the {@link EventWorkerVerticle}
 * that drains the queue into ClickHouse.
 */
public class AnalyticsVerticle extends AbstractVerticle {

    private static final Logger logger = Logger.getLogger(AnalyticsVerticle.class.getName());

    private EventPublisher publisher;


    @Override
    public void start(Promise<Void> startPromise) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        AnalyticsConfig config = AnalyticsConfig.fromEnv();

        // Connect the RabbitMQ publisher (blocking I/O) off the event loop, then
        // wire the capture service, start the HTTP server and deploy the worker.
        vertx.<Void>executeBlocking(() -> {
            publisher = new EventPublisher(config);
            publisher.start();
            return null;
        }).compose(v -> {
            CaptureService.init(vertx, publisher, config);
            return startHttpServer(port);
        }).compose(v ->
                vertx.deployVerticle(new EventWorkerVerticle(config))
        ).onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Analytics Service started successfully on port " + port);
                startPromise.complete();
            } else {
                logger.severe(() -> "Failed to start Analytics Service: " + ar.cause());
                startPromise.fail(ar.cause());
            }
        });
    }

    private io.vertx.core.Future<Void> startHttpServer(int port) {
        Promise<Void> promise = Promise.promise();
        try {
            // Create RESTEasy deployment
            VertxResteasyDeployment deployment = new VertxResteasyDeployment();
            deployment.start();
            // Create an instance of resource per Event Loop
            VertxRegistry registry = deployment.getRegistry();
            registry.addPerInstanceResource(PingResource.class);
            registry.addPerInstanceResource(OpenApiResource.class);
            registry.addPerInstanceResource(CaptureResource.class);

            deployment.setApplicationClass(AnalyticsRestApplication.class.getName());

            // Start the front end server using the Jax-RS controller
            vertx.createHttpServer()
                    .requestHandler(new VertxRequestHandler(vertx, deployment))
                    .listen(port, ar -> {
                        if (ar.succeeded()) {
                            logger.info("Analytics Service started successfully on port " + port);
                            promise.complete();
                        } else {
                            logger.severe(() -> "Failed to start Analytics Service" + ar.cause());
                            promise.fail(ar.cause());
                        }
                    });
        } catch (Exception e) {
            logger.severe(() -> "Error during verticle start" + Arrays.toString(e.getStackTrace()));
            promise.fail(e);
        }
        return promise.future();
    }

    @Override
    public void stop() {
        if (publisher != null) {
            publisher.close();
        }
    }
}
