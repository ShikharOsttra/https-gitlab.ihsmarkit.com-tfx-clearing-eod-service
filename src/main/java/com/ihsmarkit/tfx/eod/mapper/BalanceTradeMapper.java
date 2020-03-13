package com.ihsmarkit.tfx.eod.mapper;

import static com.ihsmarkit.tfx.common.math.BigDecimals.isGreaterThanZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.ihsmarkit.tfx.common.mapstruct.DefaultMapperConfig;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.LegalEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.model.BalanceTrade;

@Mapper(config = DefaultMapperConfig.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public interface BalanceTradeMapper {

    String BASE_AMOUNT_MAPPER = "baseAmountMapper";
    String VALUE_AMOUNT_MAPPER = "valueAmountMapper";
    String TRADE = "trade";

    @Mapping(target = "tradeReference", constant = "test") //FIXME proper value!!!
    @Mapping(target = "activity", constant = "NEW")
    @Mapping(target = "messageId", constant = "test")//FIXME proper value!!!
    @Mapping(target = "fixSessionId", ignore = true)
    @Mapping(target = "sequenceId", ignore = true)
    @Mapping(target = "matchingStatus", constant = "CONFIRMED")
    @Mapping(target = "errorCode", ignore = true)
    @Mapping(target = "executionTime", source = "submissionTsp")
    @Mapping(target = "submissionTsp", source = "submissionTsp")
    @Mapping(target = "accountInfo", ignore = true)
    @Mapping(target = "comments", ignore = true)
    @Mapping(target = "versionTsp", source = "submissionTsp")
    @Mapping(target = "sourceSystem", constant = "GUI")
    @Mapping(target = "utiPrefix", ignore = true)
    @Mapping(target = "utiTradeId", ignore = true)
    @Mapping(target = "userName", ignore = true)
    @Mapping(target = "clearingTsp", source = "submissionTsp")
    @Mapping(target = "matchedTradeRef", ignore = true)
    @Mapping(target = "nextVersion", ignore = true)
    @Mapping(target = "oldVersion", ignore = true)
    @Mapping(target = "matchingRef", ignore = true)
    @Mapping(target = "matchingTsp", source = "submissionTsp")
    @Mapping(target = "clearingRef", ignore = true)
    @Mapping(target = "clearingStatus", constant = "NOVATED")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currencyPair", source = TRADE)
    @Mapping(target = "baseAmount", source = TRADE, qualifiedByName = BASE_AMOUNT_MAPPER)
    @Mapping(target = "valueAmount", source = TRADE, qualifiedByName = VALUE_AMOUNT_MAPPER)
    @Mapping(target = "spotRate", source = TRADE)
    @Mapping(target = "counterparty", source = "trade.counterparty")
    @Mapping(target = "originator", source = "trade.originator")
    @Mapping(target = "productCode", source = TRADE)
    @Mapping(target = "direction", source = "trade.amount")
    @Mapping(target = "tradeDate", source = "tradeDate")
    @Mapping(target = "valueDate", source = "valueDate")
    @Mapping(target = "transactionType", constant = "BALANCE")
    @Mapping(target = "matchingOwnHash", ignore = true)
    @Mapping(target = "matchingSearchHash", ignore = true)
    @Mapping(target = "oboComment", ignore = true)
    TradeEntity toTrade(BalanceTrade trade, LocalDate tradeDate, LocalDate valueDate, LocalDateTime submissionTsp, @Context CurrencyPairEntity currencyPair,
        @Context BigDecimal spotRate);

    default Side tradeDirection(BigDecimal amount) {
        return isGreaterThanZero(amount) ? Side.BUY : Side.SELL;
    }

    default LegalEntity fromParticipant(ParticipantEntity participant) {
        return participant.getLegalEntities().get(0);
    }

    @Named(BASE_AMOUNT_MAPPER)
    default AmountEntity baseAmountMapper(BalanceTrade trade, @Context CurrencyPairEntity currencyPair) {
        return AmountEntity.of(trade.getAmount().abs(), currencyPair.getBaseCurrency());
    }

    @Named(VALUE_AMOUNT_MAPPER)
    default AmountEntity valueAmountMapper(BalanceTrade trade, @Context CurrencyPairEntity currencyPair, @Context BigDecimal spotRate) {
        return AmountEntity.of(trade.getAmount().setScale(2, RoundingMode.DOWN).abs().divide(spotRate, RoundingMode.DOWN), currencyPair.getValueCurrency());
    }

    default String currencyPairCode(BalanceTrade trade, @Context CurrencyPairEntity currencyPair) {
        return currencyPair.getCode();
    }

    default CurrencyPairEntity currencyPair(BalanceTrade trade, @Context CurrencyPairEntity currencyPair) {
        return currencyPair;
    }

    default BigDecimal spotRate(BalanceTrade trade, @Context BigDecimal spotRate) {
        return spotRate;
    }

}

