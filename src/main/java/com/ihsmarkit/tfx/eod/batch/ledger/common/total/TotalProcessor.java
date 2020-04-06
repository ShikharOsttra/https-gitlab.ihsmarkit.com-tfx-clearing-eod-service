package com.ihsmarkit.tfx.eod.batch.ledger.common.total;

import org.springframework.batch.item.ItemProcessor;

public interface TotalProcessor<I> extends ItemProcessor<I, I> {

}
