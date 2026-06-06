package com.combostrap.analyics;


import com.combostrap.analyics.config.AnalyticsConfig;
import com.combostrap.analyics.migration.Migrations;

public class Main {

    public static void main(String[] args) {

        // Bring the ClickHouse schema up to date before the service starts.
        Migrations.runUp(AnalyticsConfig.fromEnv());

        new MainLauncher().dispatch(new String[]{"run", AnalyticsVerticle.class.getName()});

    }

}
