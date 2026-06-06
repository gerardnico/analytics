package com.combostrap.analyics.migration;

import com.combostrap.analyics.config.AnalyticsConfig;
import org.apache.ibatis.migration.ConnectionProvider;
import org.apache.ibatis.migration.FileMigrationLoader;
import org.apache.ibatis.migration.JdbcConnectionProvider;
import org.apache.ibatis.migration.MigrationLoader;
import org.apache.ibatis.migration.operations.UpOperation;
import org.apache.ibatis.migration.options.DatabaseOperationOption;

import java.io.File;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Runs the ClickHouse schema migrations using MyBatis Migrations.
 * <p>
 * The migration scripts live under {@code $PROJECT_ROOT/db/scripts} (the base
 * directory is {@code db/}, the layout MyBatis Migrations expects). The same
 * scripts can be applied from the {@code migrate} CLI; here we drive the
 * {@link UpOperation} programmatically so the schema is brought up to date when
 * the service starts.
 */
public final class Migrations {

    private static final Logger LOGGER = Logger.getLogger(Migrations.class.getName());

    private Migrations() {
    }

    public static void runUp(AnalyticsConfig config) {
        File scriptsDir = new File(config.migrationScriptsDir());
        LOGGER.info("Running ClickHouse migrations from " + scriptsDir.getAbsolutePath());

        ConnectionProvider connectionProvider = new JdbcConnectionProvider(
                config.clickhouseDriver(),
                config.clickhouseUrl(),
                config.clickhouseUser(),
                config.clickhousePassword());

        MigrationLoader migrationsLoader =
                new FileMigrationLoader(scriptsDir, "UTF-8", new Properties());

        DatabaseOperationOption option = new DatabaseOperationOption();
        option.setChangelogTable("CHANGELOG");
        option.setDelimiter(";");
        // ClickHouse statements are sent whole and it has no classic transactions,
        // so commit each statement and send full scripts.
        option.setSendFullScript(true);
        option.setAutoCommit(true);
        option.setStopOnError(true);

        new UpOperation().operate(connectionProvider, migrationsLoader, option, System.out);

        LOGGER.info("ClickHouse migrations applied");
    }
}
