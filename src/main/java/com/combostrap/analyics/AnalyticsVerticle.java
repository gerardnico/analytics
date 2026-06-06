package com.combostrap.analyics;

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
 * Vert.x Verticle that configures the HTTP server with RESTEasy
 */
public class AnalyticsVerticle extends AbstractVerticle {

    private static final Logger logger = Logger.getLogger(AnalyticsVerticle.class.getName());


    @Override
    public void start(Promise<Void> startPromise) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        try {

            // Create RESTEasy deployment
            VertxResteasyDeployment deployment = new VertxResteasyDeployment();
            deployment.start();
            // Create an instance of resource per Event Loop
            VertxRegistry registry = deployment.getRegistry();
            registry.addPerInstanceResource(PingResource.class);
            registry.addPerInstanceResource(OpenApiResource.class);

            deployment.setApplicationClass(AnalyticsRestApplication.class.getName());

            // Start the front end server using the Jax-RS controller
            vertx.createHttpServer()
                    .requestHandler(new VertxRequestHandler(vertx, deployment))
                    .listen(port, ar -> {
                        if (ar.succeeded()) {
                            logger.info("Analytics Service started successfully on port " + port);
                        } else {
                            logger.severe(() -> "Failed to start Analytics Service" + ar.cause());
                            System.exit(1);
                        }
                    });

        } catch (Exception e) {
            logger.severe(() -> "Error during verticle start" + Arrays.toString(e.getStackTrace()));
            startPromise.fail(e);
        }
    }
}
