package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.eod.mapper.TradeOrPositionEssentialsMapper;
import com.ihsmarkit.tfx.eod.model.ParticipantPositionForPair;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NetCalculator {

    private final TradeOrPositionEssentialsMapper tradeOrPositionMapper;

    public Stream<ParticipantPositionForPair> netAllTtrades(final Stream<TradeEntity> trades) {
        return trades
                .map(tradeOrPositionMapper::convertTrade)
                .collect(
                        Collectors.groupingBy(
                                TradeOrPositionEssentials::getParticipant,
                                Collectors.groupingBy(
                                        TradeOrPositionEssentials::getCurrencyPair,
                                        Collectors.reducing(BigDecimal.ZERO, TradeOrPositionEssentials::getBaseAmount, BigDecimal::add)
                                )
                        )
                ).entrySet().stream()
                .flatMap(a -> a.getValue().entrySet().stream().map(b -> ParticipantPositionForPair.of(a.getKey(), b.getKey(), b.getValue())));
    }
}