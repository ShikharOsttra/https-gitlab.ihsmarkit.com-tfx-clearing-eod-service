package com.ihsmarkit.tfx.eod.batch.ledger.transactiondiary;

import org.springframework.batch.item.ItemProcessor;

import com.ihsmarkit.tfx.eod.model.ledger.TransactionDiary;

public interface TransactionDiaryLedgerProcessor<T> extends ItemProcessor<T, TransactionDiary> {

}
