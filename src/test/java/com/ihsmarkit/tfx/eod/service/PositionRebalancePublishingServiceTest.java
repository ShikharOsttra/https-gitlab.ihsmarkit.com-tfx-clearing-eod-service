package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.common.test.assertion.Matchers;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.eod.config.VelocityConfiguration;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;


@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/application.properties")
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
            aParticipantEntityBuilder()
                .type(ParticipantType.LIQUIDITY_PROVIDER)
                .notificationEmail("email1@email.com,email2@email.com, email3@email.com,            email4@email.com")
                .code("LP99")
                .build(),
            aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).status(ParticipantStatus.SUSPENDED).build()
        ));

        publishingService.publishTrades(businessDate, tradeEntities);

        verify(mailClient, times(1)).sendEmail(Matchers.argThat(emailRequest -> {
            assertThat(emailRequest.getSubject()).isEqualTo("2019-01-01 rebalance results for LP99");
            assertThat(emailRequest.getBody()).isNotEmpty();
            assertThat(emailRequest.getTo()).containsOnly("email1@email.com", "email2@email.com", "email3@email.com", "email4@email.com");
            assertThat(emailRequest.getAttachments()).hasSize(1);
        }));
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class,
        ParticipantRepository.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class, ParticipantRepository.class })
    )
    @Import(VelocityConfiguration.class)
    static class TestConfig {

    }
}
