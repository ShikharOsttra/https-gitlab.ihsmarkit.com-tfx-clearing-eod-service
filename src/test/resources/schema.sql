CREATE TABLE IF NOT EXISTS eod_ledger_collateral_list
(
    business_date           date         not null,
    trade_date              varchar(10)  not null,
    evaluation_date         varchar(10)  not null,
    record_date             varchar(19)  not null,
    participant_code        varchar(31)  not null,
    participant_name        varchar(255) not null,
    participant_type        varchar(3)   not null,
    collateral_purpose_type varchar(2)   not null,
    collateral_purpose      varchar(64)  not null,
    collateral_name         varchar(255) not null,
    collateral_type         varchar(64)  not null,
    security_code           varchar(9)   not null,
    isin_code               varchar(12)  not null,
    amount                  varchar(15)  not null,
    market_price            varchar(15)  not null,
    evaluated_price         varchar(15)  not null,
    evaluated_amount        varchar(15)  not null,
    boj_code                varchar(4)   not null,
    jasdec_code             varchar(7)   not null,
    interest_payment_day    varchar(5)   not null,
    interest_payment_day2   varchar(5)   not null,
    maturity_date           varchar(10)  not null
);

CREATE TABLE IF NOT EXISTS eod_ledger_open_position
(
    business_date               date         not null,
    trade_date                  varchar(10)  not null,
    record_date                 varchar(19)  not null,
    participant_code            varchar(31)  not null,
    participant_name            varchar(255) not null,
    participant_type            varchar(3)   not null,
    currency_no                 varchar(3)   not null,
    currency_pair               varchar(7)  not null,
    short_position_previous_day varchar(15) not null,
    long_position_previous_day  varchar(15)  not null,
    sell_trading_amount         varchar(15)  not null,
    buy_trading_amount          varchar(15)  not null,
    short_position              varchar(15)  not null,
    long_position               varchar(15)  not null,
    initial_mtm_amount          varchar(15)  not null,
    daily_mtm_amount            varchar(15)   not null,
    swap_point                  varchar(15)   not null,
    total_variation_margin      varchar(15)   not null,
    settlement_date             varchar(10)  not null
);
