package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aBondCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCashCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aCollateralBalanceEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.aLogCollateralProductEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.CollateralTestDataFactory.anEquityCollateralProductEntityBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

@ExtendWith(MockitoExtension.class)
class CollateralListLedgerProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(2019, 1, 2, 11, 30);

    @Mock
    private BojCodeProvider bojCodeProvider;

    @Mock
    private JasdecCodeProvider jasdecCodeProvider;

    @Mock
    private CollateralCalculator collateralCalculator;

    private CollateralListLedgerProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CollateralListLedgerProcessor(
            BUSINESS_DATE,
            RECORD_DATE,
            bojCodeProvider,
            jasdecCodeProvider,
            collateralCalculator
        );

        lenient().when(bojCodeProvider.getCode(any(), any())).thenReturn(Optional.of("bojc"));
        lenient().when(jasdecCodeProvider.getCode(any(), any())).thenReturn(Optional.of("jasdec1"));
    }

    @ParameterizedTest
    @MethodSource("collateralList")
    void shouldProcessBalance(final CollateralBalanceEntity balance, final CollateralListItem collateralListItem) {
        if (balance.getProduct() instanceof SecurityCollateralProductEntity) {
            when(collateralCalculator.calculateEvaluatedPrice((SecurityCollateralProductEntity) balance.getProduct())).thenReturn(new BigDecimal("10.01"));
        }

        when(collateralCalculator.calculateEvaluatedAmount(balance)).thenReturn(new BigDecimal("1300"));

        assertThat(processor.process(balance)).isEqualTo(collateralListItem);
    }

    private static Stream collateralList() {
        return Stream.of(
            Arguments.of(aCollateralBalanceEntityBuilder()
                    .product(aBondCollateralProductEntityBuilder().build())
                    .build(),
                collateralListItemBond()
            ),
            Arguments.of(aCollateralBalanceEntityBuilder()
                    .product(anEquityCollateralProductEntityBuilder().build())
                    .build(),
                collateralListItemEquity()
            ),
            Arguments.of(aCollateralBalanceEntityBuilder()
                    .product(aCashCollateralProductEntityBuilder().build())
                    .build(),
                collateralListItemCash()
            ),
            Arguments.of(aCollateralBalanceEntityBuilder()
                    .product(aLogCollateralProductEntityBuilder().build())
                    .build(),
                collateralListItemLog()
            )
        );
    }

    private static CollateralListItem collateralListItemLog() {
        return collateralListItemBuilder()
            .collateralType("LG")
            .maturityDate("2020/02/02")
            .build();
    }

    private static CollateralListItem collateralListItemCash() {
        return collateralListItemBuilder()
            .collateralType("Cash")
            .build();
    }

    private static CollateralListItem collateralListItemEquity() {
        return collateralListItemBuilder()
            .collateralName("security name")
            .collateralType("Equities")
            .securityCode("1234")
            .isinCode("JP1150481859")
            .marketPrice("1.0")
            .evaluatedPrice("10.01")
            .jasdecCode("jasdec1")
            .build();
    }

    private static CollateralListItem collateralListItemBond() {
        return collateralListItemBuilder()
            .collateralName("security name")
            .collateralType("JGB")
            .securityCode("123456789")
            .isinCode("JP1150481859")
            .marketPrice("1.0")
            .evaluatedPrice("10.01")
            .bojCode("bojc")
            .interestPaymentDay("01/01")
            .interestPaymentDay2("02/02")
            .maturityDate("2020/02/02")
            .build();
    }

    private static CollateralListItem.CollateralListItemBuilder collateralListItemBuilder() {
        return CollateralListItem.builder()
            .businessDate(BUSINESS_DATE)
            .tradeDate("2019/01/01")
            .evaluationDate("2019/01/01")
            .recordDate("2019/01/02 11:30:00")
            .participantCode("BNP")
            .participantName("BNP Paribas Securities(Japan) Limited")
            .participantType("LP")
            .collateralPurposeType("1")
            .collateralPurpose("Margin")
            .collateralName(EMPTY)
            .collateralType(EMPTY)
            .securityCode(EMPTY)
            .isinCode(EMPTY)
            .amount("1200")
            .marketPrice(EMPTY)
            .evaluatedPrice(EMPTY)
            .evaluatedAmount("1300")
            .bojCode(EMPTY)
            .jasdecCode(EMPTY)
            .interestPaymentDay(EMPTY)
            .interestPaymentDay2(EMPTY)
            .maturityDate(EMPTY);
    }

}