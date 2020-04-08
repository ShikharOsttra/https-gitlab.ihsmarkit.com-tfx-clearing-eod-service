package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aBondCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCashCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCollateralBalanceEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aLogCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.anEquityCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.anIssuerBankEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.Participant.CLEARING_HOUSE_CODE;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.BOND;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.CLEARING_DEPOSIT;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARKET_ENTRY_DEPOSIT;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants;
import com.ihsmarkit.tfx.eod.batch.ledger.ParticipantCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.SecurityCodeOrderIdProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListTfxTotalKey;

@ExtendWith(MockitoExtension.class)
class CollateralListItemOrderProviderTest {

    @Mock
    private ParticipantCodeOrderIdProvider participantCodeOrderIdProvider;
    @Mock
    private SecurityCodeOrderIdProvider securityCodeOrderIdProvider;

    @InjectMocks
    private CollateralListItemOrderProvider collateralListItemOrderProvider;

    @BeforeEach
    void init() {
        lenient().when(participantCodeOrderIdProvider.get("BNP")).thenReturn(5);
    }

    @ParameterizedTest
    @MethodSource("orderIdForOrdinaryRowDataProvider")
    void shouldProvideOrderIdForOrdinaryRow(final CollateralBalanceEntity balance, final long expectedOrderId, final Integer mockOrderId) {
        lenient().when(securityCodeOrderIdProvider.get(anyString())).thenReturn(mockOrderId);

        assertThat(collateralListItemOrderProvider.getOrderId(balance, ITEM_RECORD_TYPE)).isEqualTo(expectedOrderId);
    }

    private static Stream<Arguments> orderIdForOrdinaryRowDataProvider() {
        return Stream.of(
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aLogCollateralProductEntityBuilder()
                    .issuer(anIssuerBankEntityBuilder()
                        .subType(10)
                        .build())
                    .build()
                ).build(), 51120000000010L, 0),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aBondCollateralProductEntityBuilder()
                    .securityCode("123456789")
                    .build())
                .build(), 51130123456789L, 123456789),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(anEquityCollateralProductEntityBuilder()
                    .securityCode("000000789")
                    .build())
                .build(), 51140000000789L, 789),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aCashCollateralProductEntityBuilder().build())
                .build(), 51110000000000L, 0)
        );
    }

    @Test
    void shouldProvideOrderIdForParticipantTotalRow() {
        final CollateralListParticipantTotalKey participantTotalKey = CollateralListParticipantTotalKey.of("BNP", MARKET_ENTRY_DEPOSIT);
        assertThat(collateralListItemOrderProvider.getOrderId(participantTotalKey, LedgerConstants.SUBTOTAL_RECORD_TYPE)).isEqualTo(52400000000000L);
    }

    @Test
    void shouldProvideOrderIdForTfxTotalRow() {
        when(participantCodeOrderIdProvider.get(CLEARING_HOUSE_CODE)).thenReturn(8);
        final CollateralListTfxTotalKey tfxTotalKey = CollateralListTfxTotalKey.of(CLEARING_DEPOSIT, BOND);
        assertThat(collateralListItemOrderProvider.getOrderId(tfxTotalKey, ITEM_RECORD_TYPE)).isEqualTo(83130000000000L);
    }
}