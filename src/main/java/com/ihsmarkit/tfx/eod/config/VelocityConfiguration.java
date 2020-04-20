package com.ihsmarkit.tfx.eod.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.velocity.app.Velocity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VelocityConfiguration {

    public VelocityConfiguration(@Value("${velocity.properties.path}") final String velocityPropertiesPath) throws IOException {
        final Properties properties = new Properties();
        try (InputStream propertiesInputStream = VelocityConfiguration.class.getResourceAsStream(velocityPropertiesPath)) {
            properties.load(propertiesInputStream);
        }
        Velocity.init(properties);
    }
}
