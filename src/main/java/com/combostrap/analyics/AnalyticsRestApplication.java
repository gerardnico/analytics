package com.combostrap.analyics;

import com.combostrap.analyics.resources.CaptureResource;
import com.combostrap.analyics.resources.OpenApiResource;
import com.combostrap.analyics.resources.PingResource;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application configuration for Tower API
 */
@ApplicationPath("/")
public class AnalyticsRestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(PingResource.class);
        classes.add(OpenApiResource.class);
        classes.add(CaptureResource.class);
        return classes;
    }

}
