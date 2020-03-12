package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aBondCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCashCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCollateralBalanceEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aLogCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.anEquityCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.anIssuerBankEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
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

    @ParameterizedTest
    @MethodSource("orderIdForOrdinaryRowDataProvider")
    void shouldProvideOrderIdForOrdinaryRow(final CollateralBalanceEntity balance, final long expectedOrderId) {
        assertThat(collateralListItemOrderProvider.getOrderId(balance, LedgerConstants.ITEM_RECORD_TYPE)).isEqualTo(expectedOrderId);
    }

    private static Stream<Arguments> orderIdForOrdinaryRowDataProvider() {
        return Stream.of(
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aLogCollateralProductEntityBuilder()
                    .issuer(anIssuerBankEntityBuilder()
                        .subType(10)
                        .build())
                    .build()
                ).build(), 51120000000010L),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aBondCollateralProductEntityBuilder()
                    .securityCode("123456789")
                    .build())
                .build(), 51130123456789L),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(anEquityCollateralProductEntityBuilder()
                    .securityCode("000000789")
                    .build())
                .build(), 51140000000789L),
            Arguments.of(aCollateralBalanceEntityBuilder()
                .product(aCashCollateralProductEntityBuilder().build())
                .build(), 51110000000000L)
        );
    }

    @Test
    void shouldProvideOrderIdForTotalRow() {
        final CollateralListItemTotalKey collateralListItemTotalKey = CollateralListItemTotalKey.of("BNP", "8");
        assertThat(collateralListItemOrderProvider.getOrderId(collateralListItemTotalKey, LedgerConstants.SUBTOTAL_RECORD_TYPE)).isEqualTo(58400000000000L);
    }
}