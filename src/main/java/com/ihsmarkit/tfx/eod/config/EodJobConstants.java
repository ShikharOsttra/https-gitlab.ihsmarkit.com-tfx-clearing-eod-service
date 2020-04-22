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
    public static final String CASH_BALANCE_UPDATE_BATCH_JOB_NAME = "cashBalanceUpdateJob";
    public static final String ROLL_BUSINESS_DATE_JOB_NAME = "rollBusinessDateJob";

    public static final String BUSINESS_DATE_JOB_PARAM_NAME = "businessDate";
    public static final String CURRENT_TSP_JOB_PARAM_NAME = "currentTimestamp";
    public static final String GENERATE_MONTHLY_LEDGER_JOB_PARAM_NAME = "generateMonthlyLedger";
    public static final String MTM_TRADES_STEP_NAME = "mtmTrades";
    public static final String NET_TRADES_STEP_NAME = "netTrades";
    public static final String REBALANCE_POSITIONS_STEP_NAME = "rebalancePositions";
    public static final String ROLL_POSITIONS_STEP_NAME = "rollPositions";
    public static final String SWAP_PNL_STEP_NAME = "swapPnL";
    public static final String TOTAL_VM_STEP_NAME = "totalVM";
    public static final String MARGIN_COLLATERAL_EXCESS_OR_DEFICIENCY = "marginCollateralExcessOrDeficiency";
    public static final String COLLATERAL_LIST_LEDGER_STEP_NAME = "collateralListLedger";
    public static final String OPEN_POSITIONS_LEDGER_STEP_NAME = "openPositionsLedger";

    public static final String COLLATERAL_BALANCE_LEDGER_STEP_NAME = "collateralBalanceLedger";
    public static final String TRANSACTION_DIARY_RECORD_DATE_SET_STEP_NAME = "setTransactionDiaryRecordDateStep";
    public static final String TRADE_TRANSACTION_DIARY_LEDGER_STEP_NAME = "tradeTransactionDiaryLedger";
    public static final String SOD_TRANSACTION_DIARY_LEDGER_STEP_NAME = "sodTransactionDiaryLedger";
    public static final String NET_TRANSACTION_DIARY_LEDGER_STEP_NAME = "netTransactionDiaryLedger";
    public static final String TRANSACTION_DIARY_LEDGER_FLOW_NAME = "transactionDiaryLedger";

    public static final String DAILY_MARKET_DATA_LEDGER_STEP_NAME = "dailyMarketDataLedger";
    public static final String MONTHLY_TRADING_VOLUME_LEDGER_STEP_NAME = "monthlyTradingVolumeLedger";
    public static final String MONTHLY_TRADING_VOLUME_LEDGER_FLOW_NAME = "monthlyTradingVolumeLedgerFlow";

    public static final String CASH_BALANCE_UPDATE_STEP_NAME = "cashBalanceUpdate";
    public static final String ROLL_BUSINESS_DATE_STEP_NAME = "rollBusinessDate";
    public static final String EOD_COMPLETE_NOTIFY_STEP_NAME = "notifyOnEodCompletion";
    public static final String CASH_BALANCE_UPDATE_NOTIFY_STEP_NAME = "notifyOnCashBalanceUpdate";
}
