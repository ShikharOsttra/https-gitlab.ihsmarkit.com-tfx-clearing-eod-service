package com.ihsmarkit.tfx.eod.batch.ledger.marketdata;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aFxSpotProductEntity;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.anEodSwapPointEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.core.dl.entity.marketdata.DailySettlementPriceEntity;
import com.ihsmarkit.tfx.core.dl.repository.FxSpotProductRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository.TradeTotalAmountCurrencyPair;
import com.ihsmarkit.tfx.core.dl.repository.eod.ParticipantPositionRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.EodSwapPointRepository;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarkedDataProjection;
import com.ihsmarkit.tfx.eod.batch.ledger.marketdata.model.DailyMarketDataAggregate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
class DailyMarketDataAggregatorTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDateTime RECORD_DATE = LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1));

    @Mock
    private EodSwapPointRepository eodSwapPointRepository;
    @Mock
    private DailySettlementPriceRepository dailySettlementPriceRepository;
    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private FxSpotProductRepository fxSpotProductRepository;
    @Mock
    private ParticipantPositionRepository participantPositionRepository;

    private DailyMarketDataAggregator aggregator;

    @BeforeEach
    void beforeEach() {
        aggregator = new DailyMarketDataAggregator(
            eodSwapPointRepository, dailySettlementPriceRepository, tradeRepository, fxSpotProductRepository,
            participantPositionRepository, BUSINESS_DATE, RECORD_DATE
        );
    }

    @Test
    void shouldAggregateAllRecords() {
        mockSwapPoints();
        mockDsp();
        mockTradeTotal();
        mockOpenPositionAmount();
        mockTradingUnit();

        assertThat(aggregator.aggregate(getInputData()))
            .isNotEmpty()
            .extracting(
                DailyMarketDataAggregate::getCurrencyPairCode,
                DailyMarketDataAggregate::getBusinessDate
            )
            .containsExactlyInAnyOrder(
                tuple("USD/JPY", Date.valueOf(BUSINESS_DATE)),
                tuple("EUR/JPY", Date.valueOf(BUSINESS_DATE))
            );
    }

    private void mockSwapPoints() {
        when(eodSwapPointRepository.findAllByDateOrderedByProductNumber(any()))
            .thenReturn(List.of(
                anEodSwapPointEntityBuilder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .swapPoint(BigDecimal.ONE)
                    .build(),
                anEodSwapPointEntityBuilder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .swapPoint(BigDecimal.TEN)
                    .build()
            ));
    }

    private void mockDsp() {
        when(dailySettlementPriceRepository.findAllByBusinessDate(any()))
            .thenReturn(List.of(
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .dailySettlementPrice(new BigDecimal("1.51"))
                    .build(),
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .dailySettlementPrice(new BigDecimal("2.32"))
                    .build()
            ));

        when(dailySettlementPriceRepository.findPreviousDailySettlementPrice(any()))
            .thenReturn(List.of(
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .dailySettlementPrice(new BigDecimal("1.45"))
                    .build(),
                DailySettlementPriceEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .dailySettlementPrice(new BigDecimal("2.30"))
                    .build()
            ));
    }

    private void mockTradeTotal() {
        when(tradeRepository.findTotalBaseAmountPerCurrencyPairForBusinessDate(any()))
            .thenReturn(List.of(
                getTradeTotal("USD/JPY", new BigDecimal("1011.3")),
                getTradeTotal("EUR/JPY", new BigDecimal("117.2"))
            ));
    }

    @SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
    private TradeTotalAmountCurrencyPair getTradeTotal(final String currencyPair, final BigDecimal total) {
        // can'y mock this interface :(
        return new TradeTotalAmountCurrencyPair() {
            @Override
            public BigDecimal getTotal() {
                return total;
            }

            @Override
            public String getProductCode() {
                return currencyPair;
            }
        };
    }


    private void mockOpenPositionAmount() {
        when(participantPositionRepository.findAllByPositionTypeAndTradeDateFetchCurrencyPair(any(), any()))
            .thenReturn(List.of(
                ParticipantPositionEntity.builder()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .amount(AmountEntity.of(new BigDecimal("1111"), "JPY"))
                    .build(),
                ParticipantPositionEntity.builder()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .amount(AmountEntity.of(new BigDecimal("2222"), "JPY"))
                    .build()
            ));
    }

    private void mockTradingUnit() {
        when(fxSpotProductRepository.findAllOrderByProductNumberAsc())
            .thenReturn(List.of(
                aFxSpotProductEntity()
                    .currencyPair(currencyPair("USD", "JPY"))
                    .tradingUnit(1000L)
                    .build(),
                aFxSpotProductEntity()
                    .currencyPair(currencyPair("EUR", "JPY"))
                    .tradingUnit(1000L)
                    .build()
            ));
    }

    private static List<DailyMarkedDataProjection> getInputData() {
        return List.of(
            DailyMarkedDataProjection.of(
                1L, 1L, "USD/JPY", "101", new BigDecimal("1.1"),
                LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 1))
            ),
            DailyMarkedDataProjection.of(
                2L, 1L, "USD/JPY", "101", new BigDecimal("1000.1"),
                LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 2))
            ),
            DailyMarkedDataProjection.of(
                3L, 1L, "USD/JPY", "101", new BigDecimal("10.1"),
                LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3))
            ),


            DailyMarkedDataProjection.of(
                4L, 2L, "EUR/JPY", "102", new BigDecimal("17.1"),
                LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3))
            ),
            DailyMarkedDataProjection.of(
                5L, 2L, "EUR/JPY", "102", new BigDecimal("100.1"),
                LocalDateTime.of(BUSINESS_DATE, LocalTime.of(1, 3))
            )
        );
    }

    private static CurrencyPairEntity currencyPair(final String baseCurrency, final String valueCurrency) {
        return CurrencyPairEntity.of(1L, baseCurrency, valueCurrency);
    }

}