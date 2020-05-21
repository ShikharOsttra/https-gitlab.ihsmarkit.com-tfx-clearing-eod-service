package com.ihsmarkit.tfx.eod.batch.ledger.collateralbalance;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.CLEARING_DEPOSIT;
import static com.ihsmarkit.tfx.core.domain.type.CollateralPurpose.MARGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import com.ihsmarkit.tfx.core.dl.entity.collateral.ParticipantCollateralRequirementEntity;
import com.ihsmarkit.tfx.core.dl.repository.collateral.ParticipantCollateralRequirementRepository;
import com.ihsmarkit.tfx.eod.service.CalendarDatesProvider;

@ExtendWith(MockitoExtension.class)
class CollateralRequirementProviderTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.of(2019, 1, 1);
    private static final LocalDate NEXT_BUSINESS_DATE = LocalDate.of(2019, 1, 2);

    @Mock
    private ParticipantCollateralRequirementRepository participantCollateralRequirementRepository;

    @Mock
    private CalendarDatesProvider calendarDatesProvider;

    private CollateralRequirementProvider collateralRequirementProvider;

    @BeforeEach
    void setUp() {
        collateralRequirementProvider = new CollateralRequirementProvider(
            BUSINESS_DATE,
            participantCollateralRequirementRepository,
            calendarDatesProvider
        );
        when(calendarDatesProvider.getNextBusinessDate()).thenReturn(NEXT_BUSINESS_DATE);
    }

    @Test
    void shouldReturnRequiredAmount() {
        when(participantCollateralRequirementRepository.findNotOutdatedByBusinessDate(BUSINESS_DATE)).thenReturn(
            List.of(
                ParticipantCollateralRequirementEntity.builder()
                    .participant(aParticipantEntityBuilder().build())
                    .purpose(MARGIN)
                    .value(BigDecimal.ZERO)
                    .businessDate(LocalDate.of(2019, 1, 1))
                    .build(),
                ParticipantCollateralRequirementEntity.builder()
                    .participant(aParticipantEntityBuilder().build())
                    .purpose(MARGIN)
                    .value(BigDecimal.TEN)
                    .businessDate(LocalDate.of(2019, 1, 2))
                    .build()
            ));

        assertThat(collateralRequirementProvider.getRequiredAmount(11L)).containsOnly(Map.entry(MARGIN, BigDecimal.TEN));
    }

    @Test
    void shouldReturnNextClearingDeposit() {
        final BigDecimal nextValue = BigDecimal.TEN;
        final LocalDate applicableDate = LocalDate.of(2019, 10, 10);

        when(participantCollateralRequirementRepository.findFutureOnlyByBusinessDate(NEXT_BUSINESS_DATE)).thenReturn(
            Set.of(ParticipantCollateralRequirementEntity.builder()
                    .participant(aParticipantEntityBuilder().build())
                    .purpose(CLEARING_DEPOSIT)
                    .value(nextValue)
                    .applicableDate(applicableDate)
                    .build(),
                ParticipantCollateralRequirementEntity.builder()
                    .participant(aParticipantEntityBuilder().build())
                    .purpose(MARGIN)
                    .applicableDate(LocalDate.of(2019, 9, 9))
                    .build()
            ));

        assertThat(collateralRequirementProvider.getNextClearingDeposit(11L)).get().isEqualTo(Pair.of(applicableDate, nextValue));
    }


}