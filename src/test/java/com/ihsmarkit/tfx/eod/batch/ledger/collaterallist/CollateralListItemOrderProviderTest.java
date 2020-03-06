package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCollateralBalanceEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;

@ExtendWith(MockitoExtension.class)
class CollateralListItemOrderProviderTest {

    @Mock
    private ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;

    @InjectMocks
    private CollateralListItemOrderProvider collateralListItemOrderProvider;

    @BeforeEach
    void init() {
        when(participantCodeOrderIdProvider.get("BNP")).thenReturn(5);
    }

    @Test
    void shouldProvideOrderIdForOrdinaryRow() {
        final CollateralBalanceEntity collateralBalanceEntity = aCollateralBalanceEntityBuilder().build();
        assertThat(collateralListItemOrderProvider.getOrderId(collateralBalanceEntity, LedgerConstants.ITEM_RECORD_TYPE)).isEqualTo(5131L);
    }

    @Test
    void shouldProvideOrderIdForTotalRow() {
        final CollateralListItemTotalKey collateralListItemTotalKey = CollateralListItemTotalKey.of("BNP", "3", "8");
        assertThat(collateralListItemOrderProvider.getOrderId(collateralListItemTotalKey, LedgerConstants.SUBTOTAL_RECORD_TYPE)).isEqualTo(5384L);
    }
}