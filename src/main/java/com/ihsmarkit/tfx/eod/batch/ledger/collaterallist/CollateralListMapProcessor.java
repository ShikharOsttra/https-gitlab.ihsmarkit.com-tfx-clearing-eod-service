package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.BOND;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.EQUITY;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.SUBTOTAL_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TFX_TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.TOTAL;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalForceTwoDecimals;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatMonthDay;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.dl.entity.collateral.BondCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.LogCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.core.domain.Participant;
import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.batch.ledger.EvaluationDateProvider;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListItem;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListParticipantTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListTfxTotalKey;
import com.ihsmarkit.tfx.eod.batch.ledger.collaterallist.domain.CollateralListWriteItem;
import com.ihsmarkit.tfx.eod.batch.ledger.common.total.BigDecimalTotalValue;

import lombok.RequiredArgsConstructor;
import one.util.streamex.EntryStream;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralListMapProcessor implements ItemProcessor<CollateralListItem, CollateralListWriteItem> {

    private static final String CASH_COLLATERAL_NAME = "Yen Cash";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final BojCodeProvider bojCodeProvider;

    private final JasdecCodeProvider jasdecCodeProvider;

    private final CollateralListItemOrderProvider collateralListItemOrderProvider;

    private final EvaluationDateProvider evaluationDateProvider;

    @Override
    public CollateralListWriteItem process(final CollateralListItem item) {
        final CollateralBalanceEntity balance = item.getBalance();
        return CollateralListWriteItem.builder()
            .businessDate(businessDate)
            .tradeDate(formatDate(businessDate))
            .evaluationDate(formatDate(evaluationDateProvider.get()))
            .recordDate(formatDateTime(recordDate))
            .participantCode(balance.getParticipant().getCode())
            .participantName(balance.getParticipant().getName())
            .participantType(formatEnum(balance.getParticipant().getType()))
            .collateralPurposeType(balance.getPurpose().getValue().toString())
            .collateralPurpose(formatEnum(balance.getPurpose()))
            .collateralName(getCollateralName(balance))
            .collateralTypeNo(balance.getProduct().getType().getValue().toString())
            .collateralType(formatEnum(item.getBalance().getProduct().getType()))
            .securityCode(getFromSecurityProduct(balance.getProduct(), SecurityCollateralProductEntity::getSecurityCode))
            .isinCode(getFromSecurityProduct(balance.getProduct(), SecurityCollateralProductEntity::getIsin))
            .amount(balance.getAmount().toString())
            .marketPrice(getFromSecurityProduct(balance.getProduct(), product -> product.getEodPrice().toPlainString()))
            .evaluatedPrice(formatBigDecimalForceTwoDecimals(item.getEvaluatedPrice()))
            .evaluatedAmount(item.getEvaluatedAmount().toPlainString())
            .bojCode(getBojCode(balance))
            .jasdecCode(getJasdecCode(balance))
            .interestPaymentDay(getFromBondProduct(balance.getProduct(), product -> formatMonthDay(product.getCouponPaymentDate1())))
            .interestPaymentDay2(getFromBondProduct(balance.getProduct(), product -> formatMonthDay(product.getCouponPaymentDate2())))
            .maturityDate(getMaturityDate(balance.getProduct()))
            .recordType(ITEM_RECORD_TYPE)
            .orderId(collateralListItemOrderProvider.getOrderId(balance, ITEM_RECORD_TYPE))
            .build();
    }

    private String getJasdecCode(final CollateralBalanceEntity balance) {
        return getCustodianAccountCode(balance, EQUITY, jasdecCodeProvider::getCode);
    }

    private String getBojCode(final CollateralBalanceEntity balance) {
        return getCustodianAccountCode(balance, BOND, bojCodeProvider::getCode);
    }

    private static String getCustodianAccountCode(
        final CollateralBalanceEntity balance,
        final CollateralProductType type,
        final BiFunction<String, CollateralPurpose, Optional<String>> codeProvider
    ) {
        if (balance.getProduct().getType() != type) {
            return EMPTY;
        }

        return codeProvider.apply(balance.getParticipant().getCode(), balance.getPurpose())
            .orElse(EMPTY);
    }

    private static String getMaturityDate(final CollateralProductEntity product) {
        return product.getType() == BOND
               ? getFromBondProduct(product, bondProduct -> formatDate(bondProduct.getMaturityDate()))
               : safeGetFromProduct(product, LogCollateralProductEntity.class, lgProduct -> formatDate(lgProduct.getLogMaturityDate()));
    }

    private static String getFromSecurityProduct(final CollateralProductEntity product, final Function<SecurityCollateralProductEntity, String> mapper) {
        return safeGetFromProduct(product, SecurityCollateralProductEntity.class, mapper);
    }

    private static String getFromBondProduct(final CollateralProductEntity product, final Function<BondCollateralProductEntity, String> mapper) {
        return safeGetFromProduct(product, BondCollateralProductEntity.class, mapper);
    }

    private static <T extends CollateralProductEntity> String safeGetFromProduct(
        final CollateralProductEntity product,
        final Class<T> type,
        final Function<T, String> mapper
    ) {
        return type.isAssignableFrom(product.getClass()) ? mapper.apply(type.cast(product)) : EMPTY;
    }

    private static String getCollateralName(final CollateralBalanceEntity balance) {
        switch (balance.getProduct().getType()) {
            case CASH:
                return CASH_COLLATERAL_NAME;
            case LOG:
                return ((LogCollateralProductEntity) balance.getProduct()).getIssuer().getName();
            case BOND:
            case EQUITY:
                return getFromSecurityProduct(balance.getProduct(), SecurityCollateralProductEntity::getSecurityName);
            default:
                throw new IllegalArgumentException(String.format("Unsupported product type, %s", balance.getProduct().getType()));
        }
    }

    public List<CollateralListWriteItem> mapToParticipantTotal(final Map<CollateralListParticipantTotalKey, BigDecimalTotalValue> totals) {
        return EntryStream.of(totals)
            .mapKeyValue((collateralListParticipantTotalKey, amount) ->
                CollateralListWriteItem.builder()
                    .businessDate(businessDate)
                    .participantCode(collateralListParticipantTotalKey.getParticipantCode())
                    .collateralPurpose(TOTAL)
                    .evaluatedAmount(amount.getValue().toPlainString())
                    .orderId(collateralListItemOrderProvider.getOrderId(collateralListParticipantTotalKey, SUBTOTAL_RECORD_TYPE))
                    .recordType(SUBTOTAL_RECORD_TYPE)
                    .build()
            )
            .toList();
    }

    public List<CollateralListWriteItem> mapToTfxTotal(final Map<CollateralListTfxTotalKey, BigDecimalTotalValue> tfxTotals) {
        return EntryStream.of(tfxTotals).mapKeyValue((key, total) ->
            CollateralListWriteItem.builder()
                .businessDate(businessDate)
                .tradeDate(formatDate(businessDate))
                .evaluationDate(formatDate(evaluationDateProvider.get()))
                .recordDate(formatDateTime(recordDate))
                .participantCode(Participant.CLEARING_HOUSE_CODE)
                .participantName(TFX_TOTAL)
                .collateralPurposeType(key.getPurpose().getValue().toString())
                .collateralPurpose(formatEnum(key.getPurpose()))
                .collateralTypeNo(key.getProductType().getValue().toString())
                .collateralType(formatEnum(key.getProductType()))
                .evaluatedAmount(total.getValue().toPlainString())
                .recordType(ITEM_RECORD_TYPE)
                .orderId(collateralListItemOrderProvider.getOrderId(key, ITEM_RECORD_TYPE))
                .build()
        ).toList();
    }
}
