package com.ihsmarkit.tfx.eod.batch.ledger;

import lombok.experimental.UtilityClass;

@UtilityClass
@SuppressWarnings("checkstyle:HideUtilityClassConstructor")
public class LedgerConstants {

    public static final String PARTICIPANT_TOTAL_CONTEXT_KEY = "participantTotal";
    public static final String TFX_TOTAL_CONTEXT_KEY = "tfxTotal";

    public static final String TOTAL = "Total";
    public static final String TFX_TOTAL = "TFX Total";

    public static final int ITEM_RECORD_TYPE = 1;
    public static final int PARTICIPANT_TOTAL_RECORD_TYPE = 2;
    public static final int TOTAL_RECORD_TYPE = 3;
    public static final int SUBTOTAL_RECORD_TYPE = 4;

}
