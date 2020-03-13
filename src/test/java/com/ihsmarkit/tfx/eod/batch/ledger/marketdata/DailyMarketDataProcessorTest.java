package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL_RECORD_TYPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.time.ClockService;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarkedDataAggregated;
import com.ihsmarkit.tfx.eod.model.ledger.DailyMarketDataEnriched;
import com.ihsmarkit.tfx.eod.service.CurrencyPairSwapPointService;
import com.ihsmarkit.tfx.eod.service.FXSpotProductService;

import lombok.SneakyThrows;

@ExtendWith(MockitoExtension.class)
class DailyMarketDataProcessorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    public static final LocalDateTime DATE_TIME_2019_1_1_1_3 = LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3));
    private static final LocalDateTime DATE_TIME_2019_1_1_1_1 = LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1));
    private static final LocalDateTime DATE_TIME_2019_1_1_1_2 = LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 2));
    private static final ParticipantEntity PARTICIPANT = aParticipantEntityBuilder().build();

    @Mock
    private CurrencyPairSwapPointService currencyPairSwapPointService;
    @Mock
    private DailySettlementPriceRepository dailySettlementPriceRepository;
    @Mock
    private FXSpotProductService fxSpotProductService;
    @Mock
    private ParticipantPositionRepository participantPositionRepository;
    @Mock
    private ClockService clockService;

    private DailyMarketDataProcessor aggregator;

    @BeforeEach
    void beforeEach() {
        aggregator = new DailyMarketDataProcessor(
            currencyPairSwapPointService, dailySettlementPriceRepository, fxSpotProductService,
            participantPositionRepository, clockService, BUSINESS_DATE, DATE_TIME_2019_1_1_1_1
        );
    }

    @Test
    @SneakyThrows
    void shouldAggregateAllRecords() {
        mockClockService();
        mockSwapPoints();
        mockDsp();
        mockOpenPositionAmount();
        mockTradingUnit();
        mockCurrencyScale();

        assertThat(aggregator.process(getInputData()))
            .isNotEmpty()
            .extracting(
                DailyMarketDataEnriched::getCurrencyPairCode,
                DailyMarketDataEnriched::getBusinessDate,
                DailyMarketDataEnriched::getDspChange,
                DailyMarketDataEnriched::getSwapPoint,
                DailyMarketDataEnriched::getClosePriceTime,
                DailyMarketDataEnriched::getClosePrice,
                DailyMarketDataEnriched::getOpenPriceTime,
                DailyMarketDataEnriched::getOpenPrice,
                DailyMarketDataEnriched::getLowPriceTime,
                DailyMarketDataEnriched::getLowPrice,
                DailyMarketDataEnriched::getHighPriceTime,
                DailyMarketDataEnriched::getHighPrice,
                DailyMarketDataEnriched::getOpenPositionAmount,
                DailyMarketDataEnriched::getOpenPositionAmountInUnit,
                DailyMarketDataEnriched::getTradingVolumeAmount,
                DailyMarketDataEnriched::getTradingVolumeAmountInUnit,
                DailyMarketDataEnriched::getRecordType,
                DailyMarketDataEnriched::getOrderId
            )
            .containsExactlyInAnyOrder(
                tuple(
                    "USD/JPY", BUSINESS_DATE, "0.0600", "1.000",
                    "01:03:00", "10.1000", "01:01:00", "1.1000", "01:01:00", "1.1000", "01:02:00", "1000.1000", "0", "0", "1011", "1",
                    ITEM_RECORD_TYPE, 101L
                ),
                tuple(
                    "EUR/JPY", BUSINESS_DATE, "0.000000", "1.000",
                    "01:03:00", "100.100000", "01:03:00", "17.100000", "01:03:00", "17.100000", "01:03:00", "100.100000", "4444", "4", "117", "0",
                    ITEM_RECORD_TYPE, 102L
                ),
                tuple(
                    "Total", BUSINESS_DATE, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, "4", EMPTY, "1",
                    TOTAL_RECORD_TYPE, Long.MAX_VALUE
                )
            );
    }

    private void mockClockService() {
        when(clockService.utcTimeToServerTime(any())).then(answer -> answer.getArgument(0));
    }

    private void mockSwapPoints() {
        when(currencyPairSwapPointService.getSwapPoint(any(), anyString()))
            .thenReturn(BigDecimal.ONE);
    }

    private void mockDsp() {
        when(dailySettlementPriceRepository.findAllByBusinessDate(any()))
            .thenReturn(List.of(
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .dailySettlementPrice(new BigDecimal("1.5100000"))
                    .previousDailySettlementPrice(new BigDecimal("1.4500000"))
                    .build(),
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .dailySettlementPrice(new BigDecimal("2.3000000"))
                    .previousDailySettlementPrice(new BigDecimal("2.3000000"))
                    .build()
            ));
    }

    private void mockOpenPositionAmount() {
        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenAnswer(invocation ->
                Stream.of(
                    ParticipantPositionEntity.builder()
                        .currencyPair(currencyPair("USD", "JPY"))
                        .amount(AmountEntity.of(new BigDecimal("1111"), "JPY"))
                        .participant(PARTICIPANT)
                        .build(),
                    ParticipantPositionEntity.builder()
                        .currencyPair(currencyPair("EUR", "JPY"))
                        .amount(AmountEntity.of(new BigDecimal("-2222"), "JPY"))
                        .participant(PARTICIPANT)
                        .build()
                )
            );
    }

    private void mockTradingUnit() {
        doReturn(
            aFxSpotProductEntity()
                .currencyPair(currencyPair("USD", "JPY"))
                .tradingUnit(1000L)
                .productNumber("101")
                .build()
        ).when(fxSpotProductService).getFxSpotProduct("USD/JPY");

        doReturn(
            aFxSpotProductEntity()
                .currencyPair(currencyPair("EUR", "JPY"))
                .tradingUnit(1000L)
                .productNumber("102")
                .build()
        ).when(fxSpotProductService).getFxSpotProduct("EUR/JPY");
    }

    private void mockCurrencyScale() {
        doReturn(4).when(fxSpotProductService).getScaleForCurrencyPair("USD/JPY");
        doReturn(6).when(fxSpotProductService).getScaleForCurrencyPair("EUR/JPY");
    }

    private static Map<String, DailyMarkedDataAggregated> getInputData() {
        return Map.of(
            "USD/JPY", DailyMarkedDataAggregated.of(
                new BigDecimal("1011.3"),
                new BigDecimal("1.1"), DATE_TIME_2019_1_1_1_1,
                new BigDecimal("1000.1"), DATE_TIME_2019_1_1_1_2,
                new BigDecimal("1.1"), DATE_TIME_2019_1_1_1_1,
                new BigDecimal("10.1"), DATE_TIME_2019_1_1_1_3
            ),
            "EUR/JPY", DailyMarkedDataAggregated.of(
                new BigDecimal("117.2"),
                new BigDecimal("17.1"), DATE_TIME_2019_1_1_1_3,
                new BigDecimal("100.1"), DATE_TIME_2019_1_1_1_3,
                new BigDecimal("17.1"), DATE_TIME_2019_1_1_1_3,
                new BigDecimal("100.1"), DATE_TIME_2019_1_1_1_3
            )
        );
    }

    private static CurrencyPairEntity currencyPair(final String baseCurrency, final String valueCurrency) {
        return CurrencyPairEntity.of(1L, baseCurrency, valueCurrency);
    }

}