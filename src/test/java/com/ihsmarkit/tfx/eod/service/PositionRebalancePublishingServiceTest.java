package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aLegalEntityBuilder;
import static com.ihsmarkit.tfx.core.dl.EntityTestDataFactory.aParticipantEntityBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.ihsmarkit.tfx.common.test.assertion.Matchers;
import com.ihsmarkit.tfx.core.dl.entity.AmountEntity;
import com.ihsmarkit.tfx.core.dl.entity.CurrencyPairEntity;
import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;
import com.ihsmarkit.tfx.common.test.assertion.Matchers;
import com.ihsmarkit.tfx.core.dl.entity.TradeEntity;
import com.ihsmarkit.tfx.core.dl.repository.ParticipantRepository;
import com.ihsmarkit.tfx.core.domain.type.ParticipantStatus;
import com.ihsmarkit.tfx.core.domain.type.ParticipantType;
import com.ihsmarkit.tfx.core.domain.type.Side;
import com.ihsmarkit.tfx.eod.config.VelocityConfiguration;
import com.ihsmarkit.tfx.eod.exception.RebalancingCsvGenerationException;
import com.ihsmarkit.tfx.eod.exception.RebalancingMailSendingException;
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;


@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:/application.properties")
class PositionRebalancePublishingServiceTest {

    private static final ParticipantEntity RECIPIENT = aParticipantEntityBuilder()
        .type(ParticipantType.LIQUIDITY_PROVIDER)
        .code("LP99")
        .notificationEmail("email1@email.com,email2@email.com, email3@email.com,            email4@email.com")
        .build();

    @Autowired
    private PositionRebalancePublishingService publishingService;

    @SpyBean
    private PositionRebalanceCSVWriter positionRebalanceCSVWriter;

    @MockBean
    private AwsSesMailClient mailClient;

    @MockBean
    private ParticipantRepository participantRepository;

    @Test
    void onMailServiceCallOnEmptyTrades() {
        final List<TradeEntity> tradeEntities = List.of(aTradeBuilder());
        final LocalDate businessDate = LocalDate.of(2019, 1, 1);

        when(participantRepository.findAllNotDeletedParticipantListItems()).thenReturn(List.of(
            aParticipantEntityBuilder().type(ParticipantType.FX_BROKER).build(),
            aParticipantEntityBuilder().type(ParticipantType.CLEARING_HOUSE).build(),
            RECIPIENT,
            aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).status(ParticipantStatus.SUSPENDED).build()
        ));

        publishingService.publishTrades(businessDate, tradeEntities);

        verify(mailClient, times(1)).sendEmailWithAttachments(
            eq("2019-01-01 rebalance results for LP99"),
            anyString(),
            eq(List.of("email1@email.com", "email2@email.com", "email3@email.com", "email4@email.com")),
            Matchers.argThat(list -> assertThat(list).hasSize(1))
        );

        verify(mailClient, times(1)).sendEmail(Matchers.argThat(emailRequest -> {
            assertThat(emailRequest.getSubject()).isEqualTo("2019-01-01 rebalance results for LP99");
            assertThat(emailRequest.getBody()).isNotEmpty();
            assertThat(emailRequest.getTo()).containsOnly("email1@email.com", "email2@email.com", "email3@email.com", "email4@email.com");
            assertThat(emailRequest.getAttachments()).hasSize(1);
        }));
    }

    @Test
    void shouldSendAlert_whenCsvGenerationFails() {
        final List<TradeEntity> tradeEntities = List.of(aTradeBuilder());
        final LocalDate businessDate = LocalDate.of(2019, 1, 1);
        final Exception cause = new RuntimeException("can't generate csv exception");

        doThrow(cause).when(positionRebalanceCSVWriter).getRecordsAsCsv(anyList());

        assertThatThrownBy(() -> publishingService.publishTrades(businessDate, tradeEntities))
            .isInstanceOf(RebalancingCsvGenerationException.class)
            .hasCause(cause);

        verifyZeroInteractions(mailClient);
    }

    @Test
    void shouldSendAlert_whenEmailSendFails() {
        final List<TradeEntity> tradeEntities = List.of(aTradeBuilder());
        final LocalDate businessDate = LocalDate.of(2019, 1, 1);
        final Exception cause = new RuntimeException("can't generate csv exception");

        when(participantRepository.findAllNotDeletedParticipantListItems()).thenReturn(List.of(
            aParticipantEntityBuilder().type(ParticipantType.FX_BROKER).build(),
            aParticipantEntityBuilder().type(ParticipantType.CLEARING_HOUSE).build(),
            RECIPIENT,
            aParticipantEntityBuilder().type(ParticipantType.LIQUIDITY_PROVIDER).status(ParticipantStatus.SUSPENDED).build()
        ));
        doThrow(cause).when(mailClient).sendEmailWithAttachments(any(), any(), any(), any());

        assertThatThrownBy(() -> publishingService.publishTrades(businessDate, tradeEntities))
            .isInstanceOf(RebalancingMailSendingException.class)
            .hasCause(cause);
    }

    private TradeEntity aTradeBuilder() {
        return TradeEntity.builder()
            .currencyPair(CurrencyPairEntity.of(1L, "USD", "EUR"))
            .originator(aLegalEntityBuilder().participant(RECIPIENT).build())
            .counterparty(aLegalEntityBuilder().build())
            .direction(Side.BUY)
            .spotRate(BigDecimal.ONE)
            .baseAmount(AmountEntity.of(BigDecimal.TEN, "USD"))
            .valueAmount(AmountEntity.of(BigDecimal.TEN, "EUR"))
            .tradeDate(LocalDate.now())
            .valueDate(LocalDate.now())
            .submissionTsp(LocalDateTime.now())
            .build();
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
