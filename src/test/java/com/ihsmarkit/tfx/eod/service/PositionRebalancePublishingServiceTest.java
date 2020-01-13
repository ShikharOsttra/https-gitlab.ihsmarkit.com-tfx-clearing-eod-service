package com.ihsmarkit.tfx.eod.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.ihsmarkit.tfx.eod.service.csv.PositionRebalanceCSVWriter;
import com.ihsmarkit.tfx.mailing.client.AwsSesMailClient;

@ExtendWith(SpringExtension.class)
class PositionRebalancePublishingServiceTest {

    @Autowired
    private PositionRebalancePublishingService publishingService;

    @MockBean
    private AwsSesMailClient mailClient;

    @Test
    void onMailServiceCallOnEmptyTrades() {
        List<TradeEntity> tradeEntities = List.of();
        final LocalDate businessDate = LocalDate.of(2019, 1, 1);
        publishingService.publishTrades(businessDate, tradeEntities);

        verify(mailClient, times(0)).sendEmailWithAttachments(
            anyString(),
            eq(StringUtils.EMPTY),
            (List) argThat(IsCollectionWithSize.hasSize(1)),
            (List) argThat(IsCollectionWithSize.hasSize(1))
        );
    }

    @TestConfiguration
    @ComponentScan(basePackageClasses = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class },
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
            classes = { PositionRebalancePublishingService.class, AwsSesMailClient.class, PositionRebalanceCSVWriter.class })
    )
    static class TestConfig {

    }
}
