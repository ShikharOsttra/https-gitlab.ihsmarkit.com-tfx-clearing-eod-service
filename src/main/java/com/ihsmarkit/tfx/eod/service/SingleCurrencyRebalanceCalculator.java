package com.ihsmarkit.tfx.eod.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.eod.model.BalanceTrade;
import com.ihsmarkit.tfx.eod.model.PositionBalance;
import com.ihsmarkit.tfx.eod.model.RawPositionData;
import com.ihsmarkit.tfx.eod.model.TradeOrPositionEssentials;

@Component
public class SingleCurrencyRebalanceCalculator {

    List<BalanceTrade> rebalance(final List<TradeOrPositionEssentials> positions, final BigDecimal threshold, final int rounding) {

        PositionBalance balance = PositionBalance.of(
            positions.stream()
                .map(position -> new RawPositionData(position.getParticipant(), position.getAmount()))
        );

        final List<BalanceTrade> trades = new ArrayList<>();

        int tradesInIteration = Integer.MAX_VALUE;

        while (tradesInIteration > 0 && balance.getBuy().getNet().min(balance.getSell().getNet().abs()).compareTo(threshold) > 0) {
            final List<BalanceTrade> iterationTrades = balance.rebalance(rounding).collect(Collectors.toList());
            tradesInIteration = iterationTrades.size();
            trades.addAll(iterationTrades);
            balance = balance.applyTrades(iterationTrades.stream());
        }

        //outstanding residual amount to be allocated to LP with largest net position
        if (balance.getBuy().getNet().min(balance.getSell().getNet().abs()).compareTo(BigDecimal.ZERO) != 0) {
            final List<BalanceTrade> residualTrades = balance.allocateResidual().collect(Collectors.toList());
            trades.addAll(residualTrades);
        }

        return mergeBalanceTrades(trades);
    }

    private List<BalanceTrade> mergeBalanceTrades(final List<BalanceTrade> balanceTrades) {
        return balanceTrades.stream()
            .collect(
                Collectors.groupingBy(
                    BalanceTrade::getOriginator,
                    Collectors.groupingBy(
                        BalanceTrade::getCounterparty,
                        Collectors.reducing(BigDecimal.ZERO, BalanceTrade::getAmount, BigDecimal::add)))
            ).entrySet().stream()
            .flatMap(
                byOriginator -> byOriginator.getValue().entrySet().stream()
                    .map(byCounterpart -> new BalanceTrade(byOriginator.getKey(), byCounterpart.getKey(), byCounterpart.getValue()))
            ).collect(Collectors.toList());
    }
}
