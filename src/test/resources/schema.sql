CREATE TABLE IF NOT EXISTS eod_ledger_collateral_balance
(
    business_date                                 date         not null,
    trade_date                                    varchar(10)  not null,
    evaluation_date                               varchar(10)  not null,
    record_date                                   varchar(19)  not null,
    participant_code                              varchar(31)  not null,
    participant_name                              varchar(255) not null,
    participant_type                              varchar(3)   not null,
    collateral_purpose_type                       varchar(2)   not null,
    collateral_purpose                            varchar(64)  not null,
    total_deposit                                 varchar(30)  not null,
    cash                                          varchar(30)  not null,
    lg                                            varchar(30)  not null,
    securities                                    varchar(30)  not null,
    required_amount                               varchar(30)  not null,
    total_initial_margin                          varchar(30)  not null,
    total_variation_margin                        varchar(30)  not null,
    total_excess_deficit                          varchar(30)  not null,
    deficit_in_cash_settlement                    varchar(30)  not null,
    cash_settlement                               varchar(30)  not null,
    cash_settlement_following_day                 varchar(30)  not null,
    initial_mtm_total                             varchar(30)  not null,
    initial_mtm_day                               varchar(30)  not null,
    initial_mtm_following_day                     varchar(30)  not null,
    daily_mtm_total                               varchar(30)  not null,
    daily_mtm_day                                 varchar(30)  not null,
    daily_mtm_following_day                       varchar(30)  not null,
    swap_point_total                              varchar(30)  not null,
    swap_point_day                                varchar(30)  not null,
    swap_point_following_day                      varchar(30)  not null,
    following_applicable_day_for_clearing_deposit varchar(10)  not null,
    record_type                                   int          not null,
    order_id                                      bigint       not null
);

CREATE TABLE IF NOT EXISTS eod_ledger_collateral_list
(
    business_date           date         not null,
    trade_date              varchar(10)  not null,
    evaluation_date         varchar(10)  not null,
    record_date             varchar(19)  not null,
    participant_code        varchar(31)  not null,
    participant_name        varchar(255) not null,
    participant_type        varchar(3)   not null,
    collateral_purpose_type varchar(5)   not null,
    collateral_purpose      varchar(64)  not null,
    collateral_name         varchar(255) not null,
    collateral_type_no      varchar(1)   not null,
    collateral_type         varchar(64)  not null,
    security_code           varchar(9)   not null,
    isin_code               varchar(12)  not null,
    amount                  varchar(30)  not null,
    market_price            varchar(30)  not null,
    evaluated_price         varchar(30)  not null,
    evaluated_amount        varchar(30)  not null,
    boj_code                varchar(4)   not null,
    jasdec_code             varchar(7)   not null,
    interest_payment_day    varchar(5)   not null,
    interest_payment_day2   varchar(5)   not null,
    maturity_date           varchar(10)  not null,
    record_type             int          not null,
    order_id                bigint       not null
);

CREATE TABLE IF NOT EXISTS eod_ledger_market_data
(
    business_date          date        not null,
    trade_date             varchar(10) not null,
    record_date            varchar(19) not null,
    currency_no            varchar(3)  not null,
    currency_pair          varchar(7)  not null,
    previous_day_dsp       varchar(30) not null,
    open_price             varchar(30) not null,
    open_price_time        varchar(8)  not null,
    high_price             varchar(30) not null,
    high_price_time        varchar(8)  not null,
    low_price              varchar(30) not null,
    low_price_time         varchar(8)  not null,
    close_price            varchar(30) not null,
    close_price_time       varchar(8)  not null,
    dsp                    varchar(15) not null,
    dsp_change             varchar(30) not null,
    swap_point             varchar(30) not null,
    trading_volume_in_unit varchar(30) not null,
    trading_volume_amount  varchar(30) not null,
    open_position_in_unit  varchar(30) not null,
    open_position_amount   varchar(30) not null,
    record_type            int         not null,
    order_id               bigint      not null
);

CREATE TABLE IF NOT EXISTS eod_ledger_monthly_trading_volume
(
    business_date               date         not null,
    trade_date                  varchar(10)  not null,
    record_date                 varchar(19)  not null,
    participant_code            varchar(31)  not null,
    participant_name            varchar(255) not null,
    participant_type            varchar(3)   not null,
    currency_no                 varchar(3)   not null,
    currency_pair               varchar(7)   not null,
    sell_trading_volume_in_unit varchar(30)  not null,
    buy_trading_volume_in_unit  varchar(30)  not null
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
    currency_pair               varchar(21)  not null,
    short_position_previous_day varchar(30)  not null,
    long_position_previous_day  varchar(30)  not null,
    sell_trading_amount         varchar(30)  not null,
    buy_trading_amount          varchar(30)  not null,
    short_position              varchar(30)  not null,
    long_position               varchar(30)  not null,
    initial_mtm_amount          varchar(30)  not null,
    daily_mtm_amount            varchar(30)  not null,
    swap_point                  varchar(30)  not null,
    total_variation_margin      varchar(30)  not null,
    settlement_date             varchar(30)  not null,
    record_type                 int          not null,
    order_id                    bigint       not null
);

CREATE TABLE IF NOT EXISTS eod_ledger_transaction_diary
(
    business_date               date         not null,
    trade_date                  varchar(10)  not null,
    record_date                 varchar(19)  not null,
    participant_code            varchar(31)  not null,
    participant_name            varchar(255) not null,
    participant_type            varchar(3)   not null,
    currency_no                 varchar(3)   not null,
    currency_pair               varchar(7)   not null,
    match_date                  varchar(10)  not null,
    match_time                  varchar(8)   not null,
    match_id                    varchar(24)  not null,
    clear_date                  varchar(10)  not null,
    clear_time                  varchar(8)   not null,
    clearing_id                 varchar(8)   not null,
    trade_price                 varchar(30)  not null,
    sell_amount                 varchar(30)  not null,
    buy_amount                  varchar(30)  not null,
    counterparty_code           varchar(15)  not null,
    counterparty_type           varchar(3)   not null,
    dsp                         varchar(30)  not null,
    daily_mtm_amount            varchar(30)  not null,
    swap_point                  varchar(30)  not null,
    outstanding_position_amount varchar(30)  not null,
    settlement_date             varchar(10)  not null,
    trade_id                    varchar(40)  not null,
    trade_type                  varchar(2)   not null,
    reference                   varchar(40)  not null,
    user_reference              varchar(255) not null
);