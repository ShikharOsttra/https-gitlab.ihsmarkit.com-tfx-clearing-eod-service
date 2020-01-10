package com.ihsmarkit.tfx.eod.config;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@RequiredArgsConstructor
class SwaggerConfig {

    private final ErrorController errorController;
    private final WebEndpointProperties webEndpointProperties;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(
                and(
                    not(PathSelectors.regex(errorController.getErrorPath())),
                    not(PathSelectors.regex(webEndpointProperties.getBasePath().concat(".*")))
                )
            )
            .build();
    }
}

