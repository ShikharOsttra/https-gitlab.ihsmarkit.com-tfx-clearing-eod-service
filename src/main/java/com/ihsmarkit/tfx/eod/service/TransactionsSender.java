package com.ihsmarkit.tfx.eod.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ihsmarkit.tfx.core.domain.TradeStateMachineResponse;
import com.ihsmarkit.tfx.core.domain.transaction.NewTransactionsRequest;
import com.ihsmarkit.tfx.core.jms.JmsProcessorsFactory;
import com.ihsmarkit.tfx.core.jms.SyncJmsProcessor;
import com.ihsmarkit.tfx.eod.exception.EodJobException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TransactionsSender {

    private final SyncJmsProcessor<NewTransactionsRequest, TradeStateMachineResponse> syncJmsProcessor;

    public TransactionsSender(
        @Value("${jms.trade-state-machine.new-transaction-input.queue}") final String requestDestination,
        final JmsProcessorsFactory jmsProcessorsFactory
    ) {
        this.syncJmsProcessor = jmsProcessorsFactory.synchronous(requestDestination, TradeStateMachineResponse.class);
    }

    public void send(final NewTransactionsRequest request) {
        syncJmsProcessor.doRequest(request)
            .filter(TradeStateMachineResponse::isError)
            .ifPresent(error -> {
                log.error("Error while sending new transaction, request: {}, response: {}", request, error);
                throw new EodJobException("Cannot create new transaction, reason: " + error.getMessage());
            });
    }

}
