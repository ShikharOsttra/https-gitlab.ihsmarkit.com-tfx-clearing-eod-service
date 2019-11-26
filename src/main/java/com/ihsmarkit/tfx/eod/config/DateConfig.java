package com.ihsmarkit.tfx.eod.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

import com.ihsmarkit.tfx.eod.service.BusinessDateConverter;

@Configuration
public class DateConfig {

    @Bean
    public FormattingConversionService conversionService() {
        final DefaultFormattingConversionService conversionService =
            new DefaultFormattingConversionService(false);

        conversionService.addConverter(new BusinessDateConverter());

        return conversionService;
    }

}
