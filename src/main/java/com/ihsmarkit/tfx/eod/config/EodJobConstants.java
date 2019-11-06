package com.ihsmarkit.tfx.eod.config;

import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EodJobConstants {

    public static final DateTimeFormatter BUSINESS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String JPY = "JPY";

    public static final String JST = "Asia/Tokyo";

    public static final String EOD1_BATCH_JOB_NAME = "eod1Job";

    public static final String BUSINESS_DATE_JOB_PARAM_NAME = "businessDate";
}
