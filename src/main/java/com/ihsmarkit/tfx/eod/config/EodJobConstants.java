package com.ihsmarkit.tfx.eod.config;

import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EodJobConstants {

    public static final DateTimeFormatter BUSINESS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String USD = "USD";

    public static final String JPY = "JPY";

    public static final String BUSINESS_DATE_JOB_PARAM_NAME = "businessDate";

    public static final String MTM_TRADES_STEP_NAME = "mtmTrades";

    public static final String NET_TRADES_STEP_NAME = "netTrades";

}
