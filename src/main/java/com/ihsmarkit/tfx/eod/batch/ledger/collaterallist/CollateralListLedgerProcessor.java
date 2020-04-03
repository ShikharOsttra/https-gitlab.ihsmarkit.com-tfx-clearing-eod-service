package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.BOND;
import static com.ihsmarkit.tfx.core.domain.type.CollateralProductType.EQUITY;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerConstants.ITEM_RECORD_TYPE;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatBigDecimalForceTwoDecimals;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDate;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatDateTime;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatEnum;
import static com.ihsmarkit.tfx.eod.batch.ledger.LedgerFormattingUtils.formatMonthDay;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.ihsmarkit.tfx.core.domain.type.CollateralProductType;
import com.ihsmarkit.tfx.core.domain.type.CollateralPurpose;
import com.ihsmarkit.tfx.eod.batch.ledger.EvaluationDateProvider;
import com.ihsmarkit.tfx.eod.model.ledger.CollateralListItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@StepScope
public class CollateralListLedgerProcessor implements ItemProcessor<CollateralBalanceEntity, CollateralListItem<BigDecimal>> {

    private static final String CASH_COLLATERAL_NAME = "Yen Cash";

    @Value("#{jobParameters['businessDate']}")
    private final LocalDate businessDate;

    @Value("#{stepExecutionContext['recordDate']}")
    private final LocalDateTime recordDate;

    private final BojCodeProvider bojCodeProvider;

    private final JasdecCodeProvider jasdecCodeProvider;

    private final CollateralCalculator collateralCalculator;

    private final CollateralListItemOrderProvider collateralListItemOrderProvider;

    private final EvaluationDateProvider evaluationDateProvider;

    @Override
    public CollateralListItem<BigDecimal> process(final CollateralBalanceEntity balance) {
        final BigDecimal evaluatedAmount = collateralCalculator.calculateEvaluatedAmount(balance);
        return CollateralListItem.<BigDecimal>builder()
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
            .collateralType(formatEnum(balance.getProduct().getType()))
            .securityCode(getFromSecurityProduct(balance.getProduct(), SecurityCollateralProductEntity::getSecurityCode))
            .isinCode(getFromSecurityProduct(balance.getProduct(), SecurityCollateralProductEntity::getIsin))
            .amount(balance.getAmount().toString())
            .marketPrice(getFromSecurityProduct(balance.getProduct(), product -> product.getEodPrice().toPlainString()))
            .evaluatedPrice(getFromSecurityProduct(balance.getProduct(),
                product -> formatBigDecimalForceTwoDecimals(collateralCalculator.calculateEvaluatedPrice(product))
            ))
            .evaluatedAmount(evaluatedAmount)
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
}
