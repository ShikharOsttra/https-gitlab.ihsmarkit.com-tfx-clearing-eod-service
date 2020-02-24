package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static com.ihsmarkit.tfx.core.domain.type.TransactionType.REGULAR;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;

@ExtendWith(SpringExtension.class)
class PositionRebalancePublishingServiceTest {

    @Autowired
    private PositionRebalancePublishingService publishingService;

    @MockBean
    private AwsSesMailClient mailClient;

    @MockBean
    private ParticipantRepository participantRepository;

    @Test
    void onMailServiceCallOnEmptyTrades() {
        List<TradeEntity> tradeEntities = List.of();
        final LocalDate businessDate = LocalDate.of(2019, 1, 1);
        when(participantRepository.findAllNotDeletedParticipantListItems()).thenReturn(List.of(
            aParticipantEntityBuilder().type(ParticipantType.FX_BROKER).build(),
            aParticipantEntityBuilder().type(ParticipantType.CLEARING_HOUSE).build(),
            aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).code("LP99").build(),
            aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).status(ParticipantStatus.SUSPENDED).build()
        ));

        publishingService.publishTrades(businessDate, tradeEntities);

        verify(mailClient, times(1)).sendEmailWithAttachments(
            anyString(),
            eq(StringUtils.EMPTY),
            (List) argThat(IsCollectionWithSize.hasSize(1)),
            (List) argThat(IsCollectionWithSize.hasSize(1))
        );
    }

    private TradeEntity.TradeEntityBuilder aTradeBuilder() {
        return TradeEntity.builder()
            .tradeReference("tradeRef")
            .transactionType(REGULAR);
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class,
        ParticipantRepository.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class, ParticipantRepository.class })
    )
    static class TestConfig {

    }
}
