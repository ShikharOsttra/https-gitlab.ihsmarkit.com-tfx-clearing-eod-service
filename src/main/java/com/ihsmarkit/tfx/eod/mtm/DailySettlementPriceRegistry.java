package com.ihsmarkit.tfx.eod.mtm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.entity.eod.ParticipantPositionEntity;
import com.ihsmarkit.tfx.eod.mtm.generated.TradeOrPositionEssentialsMapperImpl;

public class DailySettlementPriceRegistry {

    private TradeOrPositionEssentialsMapper tradeOrPositionMapper = //Mappers.getMapper(TradeOrPositionEssentialsMapper.class);
            new TradeOrPositionEssentialsMapperImpl();

    private final Map<String, BigDecimal> JPYRates;
    private final Map<CurrencyPairEntity, BigDecimal> rateByCurrencyPair;

    public DailySettlementPriceRegistry(Map<CurrencyPairEntity, BigDecimal> rateByCurrencyPair) {
        this.rateByCurrencyPair = rateByCurrencyPair;
        this.JPYRates = rateByCurrencyPair.entrySet().stream()
                .filter((a) -> a.getKey().getValueCurrency().equals("JPY"))
                .collect(Collectors.toMap(
                        (a) -> a.getKey().getBaseCurrency(),
                        Map.Entry::getValue
                ));
    }

    public TradeMTM calculateMTM(TradeEntity trade) {
        return calculateMTM(tradeOrPositionMapper.convertTrade(trade));
    }

    private TradeMTM calculateMTM(TradeOrPositionEssentials trade) {

        var currencyPair = trade.getCurrencyPair();
        final String valueCurrency = currencyPair.getValueCurrency();

        var rate = rateByCurrencyPair.get(currencyPair);

        var mtmAmount = rate.subtract(trade.getSpotRate()).multiply(trade.getBaseAmount());

        if (!valueCurrency.equals("JPY")) {
            mtmAmount = mtmAmount.multiply(JPYRates.get(valueCurrency));
        }

        mtmAmount = mtmAmount.setScale(0, RoundingMode.FLOOR);

        return TradeMTM.of(trade.getParticipant(), currencyPair, mtmAmount);
    }

    public Stream<TradeMTM> calculateAndAggregateInitialMTM(Stream<TradeEntity> trades) {

        return trades
                .map(tradeOrPositionMapper::convertTrade)
                .map(this::calculateMTM)
                .collect(
                        Collectors.groupingBy(
                                TradeMTM::getParticipant,
                                Collectors.groupingBy(
                                        TradeMTM::getCurrencyPair,
                                        Collectors.reducing(BigDecimal.ZERO, TradeMTM::getAmount, BigDecimal::add)
                                )
                        )
                ).entrySet().stream()
                    .flatMap( (a) -> a.getValue().entrySet().stream().map( (b) -> TradeMTM.of(a.getKey(), b.getKey(), b.getValue())));
    }

    public Stream<TradeMTM> calculateAndAggregateDailyMTM(Collection<ParticipantPositionEntity> positions) {

        return positions.stream()
                .map(tradeOrPositionMapper::convertPosition)
                .map(this::calculateMTM);
    }

}
