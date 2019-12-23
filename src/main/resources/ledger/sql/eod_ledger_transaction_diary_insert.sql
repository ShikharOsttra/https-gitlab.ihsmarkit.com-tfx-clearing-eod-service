INSERT INTO eod_ledger_transaction_diary(business_date,
										trade_date,
										record_date,
										participant_code,
										participant_name,
										participant_type,
										currency_no,
										currency_pair,
										match_date,
										match_time,
										match_id,
										clear_date,
										clear_time,
										clearing_id,
										trade_price,
										sell_amount,
										buy_amount,
										counterparty_code,
										counterparty_type,
										dsp,
										daily_mtm_amount,
										swap_point,
										outstanding_position_amount,
										settlement_date,
										trade_id,
										trade_type,
										reference,
										user_reference)

VALUES (:business_date,
		:trade_date,
		:record_date,
		:participant_code,
		:participant_name,
		:participant_type,
		:currency_no,
		:currency_pair,
		:match_date,
		:match_time,
		:match_id,
		:clear_date,
		:clear_time,
		:clearing_id,
		:trade_price,
		:sell_amount,
		:buy_amount,
		:counterparty_code,
		:counterparty_type,
		:dsp,
		:daily_mtm_amount,
		:swap_point,
		:outstanding_position_amount,
		:settlement_date,
		:trade_id,
		:trade_type,
		:reference,
		:user_reference);