package com.combostrap.analyics;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * A custom <a href="https://vertx.io/docs/vertx-core/java/#_the_vert_x_launcher">Vertx Launcher</a>
 * (to set environment before launching)
 * <p></p>
 * The vert.x  Launcher is used in:
 * * fat jar as main class,
 * * and by the vertx command line utility.
 * There is also a cli API
 * <a href="https://vertx.io/docs/vertx-core/java/#_vert_x_command_line_interface_api">CLI</a>
 * <p>
 * The launcher can be used in a main method.
 * ```java
 * new MainLauncher().dispatch(new String[]{"run", SmtpVerticle.class.getName()});
 * ```
 * Don't call {@link MainLauncher#executeCommand(String, String...)}, otherwise the hooks are not called
 */
public class MainLauncher extends io.vertx.core.Launcher {

    static final Logger LOGGER = Logger.getLogger(MainLauncher.class.getName());

    public static final VertxPrometheusMetrics prometheus = new VertxPrometheusMetrics();


    @Override
    public void beforeStartingVertx(VertxOptions options) {
        super.beforeStartingVertx(options);
        LOGGER.info("Enabling Metrics");
        options
                .setMetricsOptions(prometheus.getInitMetricsOptions());
        LOGGER.info("Workers default max execution time");
        options
                .setMaxWorkerExecuteTime(30) // default 60 seconds
                .setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS); // default nanoseconds
    }


    @Override
    public void afterStartingVertx(Vertx vertx) {
        super.afterStartingVertx(vertx);
        LOGGER.info("Enabling Histogram Metrics");
        prometheus.configEnableHistogramBuckets();
        LOGGER.info("Enabling Jvm Metrics");
        prometheus.configEnableJvm();
    }

    /**
     * The arguments are taken from the fat jar
     *
     * @param args - the arguments to dispatch
     */
    public static void main(String[] args) {

        new MainLauncher().dispatch(args);

    }

}
