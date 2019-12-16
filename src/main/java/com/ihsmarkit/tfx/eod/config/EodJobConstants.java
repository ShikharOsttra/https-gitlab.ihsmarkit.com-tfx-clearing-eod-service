package com.ihsmarkit.tfx.eod.config;

import java.time.format.DateTimeFormatter;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EodJobConstants {

    public static final DateTimeFormatter BUSINESS_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static final String USD = "USD";
    public static final String EUR = "EUR";
    public static final String JPY = "JPY";

    public static final String EOD1_BATCH_JOB_NAME = "eod1Job";
    public static final String EOD2_BATCH_JOB_NAME = "eod2Job";

    public static final String JOB_NAME_PARAM_NAME = "jobName";
    public static final String BUSINESS_DATE_JOB_PARAM_NAME = "businessDate";
    public static final String CURRENT_TSP_JOB_PARAM_NAME = "currentTimestamp";
    public static final String MTM_TRADES_STEP_NAME = "mtmTrades";
    public static final String NET_TRADES_STEP_NAME = "netTrades";
    public static final String REBALANCE_POSITIONS_STEP_NAME = "rebalancePositions";
    public static final String ROLL_POSITIONS_STEP_NAME = "rollPositions";
    public static final String SWAP_PNL_STEP_NAME = "swapPnL";
    public static final String COLLATERAL_LIST_LEDGER_STEP_NAME = "collateralListLedger";

}
