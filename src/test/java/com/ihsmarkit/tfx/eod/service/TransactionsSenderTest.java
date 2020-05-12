package com.ihsmarkit.tfx.eod.service;

import static com.ihsmarkit.tfx.common.test.assertion.Matchers.argThat;
import static com.ihsmarkit.tfx.core.domain.TradeStateMachineResponse.success;
import static com.ihsmarkit.tfx.core.domain.TradeStateMachineResponse.unknownError;
import static com.ihsmarkit.tfx.core.test.TestDataFactory.aNewTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihsmarkit.tfx.core.domain.transaction.NewTransactionsRequest;
import com.ihsmarkit.tfx.core.jms.JmsProcessorsFactory;
import com.ihsmarkit.tfx.core.support.AbstractJmsTest;
import com.ihsmarkit.tfx.eod.exception.EodJobException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.SneakyThrows;

@ContextConfiguration(classes = {
    TransactionsSender.class,
    JmsProcessorsFactory.class,
    TransactionsSenderTest.ResponseStubber.class
})
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
class TransactionsSenderTest extends AbstractJmsTest {

    @Autowired
    private TransactionsSender transactionsSender;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private ResponseStubber responseStubber;

    @Test
    @SneakyThrows
    void shouldSendNotificationToValidQueue() {
        final var request = NewTransactionsRequest.builder().transactions(List.of(aNewTransaction().build())).build();

        responseStubber.mockResponse(success());

        transactionsSender.send(request);

        verify(responseStubber).receiveAndRespondWith(argThat(actual -> {
            final var expectedValue = objectMapper.valueToTree(request);
            final var actualValue = objectMapper.valueToTree(actual);
            assertThat(expectedValue).isEqualTo(actualValue);
        }));
    }

    @Test
    @SneakyThrows
    void shouldThrowExceptionWhenResponseIsError() {
        final var request = NewTransactionsRequest.builder().transactions(List.of(aNewTransaction().build())).build();

        responseStubber.mockResponse(unknownError("error"));

        assertThatCode(() -> transactionsSender.send(request))
            .isInstanceOf(EodJobException.class)
            .hasMessage("Cannot create new transaction, reason: error");
    }

    @Component
    static class ResponseStubber {

        private final AtomicReference<Object> responseStub = new AtomicReference<>(success());

        @JmsListener(destination = "${jms.trade-state-machine.new-transaction-input.queue}")
        Object receiveAndRespondWith(final NewTransactionsRequest request) {
            return responseStub.get();
        }

        public void mockResponse(final Object response) {
            responseStub.set(response);
        }
    }
}
