INSERT INTO eod_ledger_collateral_balance(business_date,
                                          trade_date,
                                          record_date,
                                          participant_code,
                                          participant_name,
                                          participant_type,
                                          collateral_purpose_type,
                                          collateral_purpose,
                                          total_deposit,
                                          cash,
                                          lg,
                                          securities,
                                          required_amount,
                                          total_initial_margin,
                                          total_variation_margin,
                                          total_excess_deficit,
                                          deficit_in_cash_settlement,
                                          cash_settlement,
                                          cash_settlement_following_day,
                                          initial_mtm_total,
                                          initial_mtm_day,
                                          initial_mtm_following_day,
                                          daily_mtm_total,
                                          daily_mtm_day,
                                          daily_mtm_following_day,
                                          swap_point_total,
                                          swap_point_day,
                                          swap_point_following_day,
                                          following_applicable_day_for_clearing_deposit,
                                          record_type,
                                          order_id)
VALUES (:businessDate,
        :tradeDate,
        :recordDate,
        :participantCode,
        :participantName,
        :participantType,
        :collateralPurposeType,
        :collateralPurpose,
        :totalDeposit,
        :cash,
        :lg,
        :securities,
        :requiredAmount,
        :totalInitialMargin,
        :totalVariationMargin,
        :totalExcessDeficit,
        :deficitInCashSettlement,
        :cashSettlement,
        :cashSettlementFollowingDay,
        :initialMtmTotal,
        :initialMtmDay,
        :initialMtmFollowingDay,
        :dailyMtmTotal,
        :dailyMtmDay,
        :dailyMtmFollowingDay,
        :swapPointTotal,
        :swapPointDay,
        :swapPointFollowingDay,
        :followingApplicableDayForClearingDeposit,
        :recordType,
        :orderId)