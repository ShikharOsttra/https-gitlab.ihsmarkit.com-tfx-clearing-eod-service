package com.ihsmarkit.tfx.eod.batch.ledger;

import java.util.List;

public interface TotalSupplier<T> {

    List<T> get();

}
