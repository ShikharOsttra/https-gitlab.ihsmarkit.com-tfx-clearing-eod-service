package com.ihsmarkit.tfx.eod.batch.ledger.collaterallist;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.collateral.calculator.CollateralCalculatorFactory;
import com.ihsmarkit.tfx.collateral.calculator.EvaluatedAmountCalculator;
import com.ihsmarkit.tfx.collateral.calculator.EvaluatedUnitPriceCalculator;
import com.ihsmarkit.tfx.collateral.calculator.domain.ProductDetailsAdapterFactory;
import com.ihsmarkit.tfx.core.dl.entity.collateral.CollateralBalanceEntity;
import com.ihsmarkit.tfx.core.dl.entity.collateral.SecurityCollateralProductEntity;

@Component
@StepScope
public class CollateralCalculator {

    private final EvaluatedAmountCalculator evaluatedAmountCalculator;

    private final EvaluatedUnitPriceCalculator evaluatedUnitPriceCalculator;

    public CollateralCalculator(final CollateralCalculatorFactory collateralCalculatorFactory) {
        this.evaluatedAmountCalculator = collateralCalculatorFactory.getEvaluatedAmountCalculator();
        this.evaluatedUnitPriceCalculator = collateralCalculatorFactory.getEvaluatedUnitPriceCalculator();
    }

    public BigDecimal calculateEvaluatedAmount(final CollateralBalanceEntity balance) {
        return evaluatedAmountCalculator.calculate(balance.getAmount(), ProductDetailsAdapterFactory.fromEntity(balance.getProduct()))
            .setScale(0, RoundingMode.DOWN);
    }

    public BigDecimal calculateEvaluatedPrice(final SecurityCollateralProductEntity securityProduct) {
        return evaluatedUnitPriceCalculator.calculate(ProductDetailsAdapterFactory.fromEntity(securityProduct));
    }
}
