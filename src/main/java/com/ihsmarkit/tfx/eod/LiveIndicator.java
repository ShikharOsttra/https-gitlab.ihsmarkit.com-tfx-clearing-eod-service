package com.ihsmarkit.tfx.eod;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LiveIndicator {
    @Scheduled(fixedRateString = "PT0.5S")
    public void check() {
        log.info("Live, {}", LocalDateTime.now(ZoneOffset.UTC));
    }
}
