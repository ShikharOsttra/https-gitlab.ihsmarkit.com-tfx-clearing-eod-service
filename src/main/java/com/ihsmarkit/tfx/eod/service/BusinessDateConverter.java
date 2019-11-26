package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.eod.config.EodJobConstants.BUSINESS_DATE_FMT;

import java.time.LocalDate;

import org.springframework.core.convert.converter.Converter;

public class BusinessDateConverter implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(final String source) {
        return LocalDate.parse(source, BUSINESS_DATE_FMT);
    }
}
