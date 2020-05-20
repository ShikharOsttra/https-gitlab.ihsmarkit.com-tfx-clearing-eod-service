package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import static java.util.stream.Collectors.toMap;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.collateral.CollateralCalculatorFactory;
import com.ihsmarkit.tfx.core.collateral.EvaluatedAmountCalculator;
import com.ihsmarkit.tfx.core.collateral.EvaluatedUnitPriceCalculator;
import com.ihsmarkit.tfx.core.collateral.domain.ProductDetailsAdapterFactory;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.haircut.HaircutRateEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.HaircutRateRepository;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;

@Component
@StepScope
public class CollateralCalculator {

    private final EvaluatedAmountCalculator evaluatedAmountCalculator;

    private final EvaluatedUnitPriceCalculator evaluatedUnitPriceCalculator;

    public CollateralCalculator(
        final CollateralCalculatorFactory collateralCalculatorFactory,
        @Value("#{jobParameters['businessDate']}") final LocalDate businessDate,
        final CalendarDatesProvider calendarDatesProvider,
        final HaircutRateRepository haircutRateRepository
    ) {

        final LocalDate nextBusinessDate = calendarDatesProvider.getNextBusinessDate();

        final Collection<HaircutRateEntity> haircutRateEntities = haircutRateRepository.findNotOutdatedByBusinessDate(businessDate).stream()
            .filter(rate -> !rate.getBusinessDate().isAfter(nextBusinessDate))
            .collect(
                toMap(
                    HaircutRateEntity::getKey,
                    Function.identity(),
                    BinaryOperator.maxBy(Comparator.comparing(HaircutRateEntity::getBusinessDate))
                )
            ).values();

        evaluatedAmountCalculator = collateralCalculatorFactory.getEvaluatedAmountCalculator(haircutRateEntities);
        evaluatedUnitPriceCalculator = collateralCalculatorFactory.getEvaluatedUnitPriceCalculator(haircutRateEntities);
    }

    public BigDecimal calculateEvaluatedAmount(final CollateralBalanceEntity balance) {
        return evaluatedAmountCalculator.calculate(balance.getAmount(), ProductDetailsAdapterFactory.fromEntity(balance.getProduct()))
            .setScale(0, RoundingMode.DOWN);
    }

    public BigDecimal calculateEvaluatedPrice(final SecurityCollateralProductEntity securityProduct) {
        return evaluatedUnitPriceCalculator.calculate(ProductDetailsAdapterFactory.fromEntity(securityProduct));
    }
}
