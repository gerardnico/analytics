package com.combostrap.analyics;


public class Main {

    public static void main(String[] args) {

        new MainLauncher().dispatch(new String[]{"run", AnalyticsVerticle.class.getName()});

    }

}
