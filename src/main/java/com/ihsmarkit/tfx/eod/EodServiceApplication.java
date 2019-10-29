package com.ihsmarkit.tfx.eod;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@EnableScheduling
@SpringBootApplication
public class EodServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(EodServiceApplication.class, args);
    }
}
