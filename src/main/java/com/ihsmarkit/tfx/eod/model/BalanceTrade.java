package com.ihsmarkit.tfx.eod.model;

import java.math.BigDecimal;

import com.ihsmarkit.tfx.core.dl.entity.ParticipantEntity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class BalanceTrade {
    private final ParticipantEntity originator;
    private final ParticipantEntity counterpart;
    private final BigDecimal amount;
}