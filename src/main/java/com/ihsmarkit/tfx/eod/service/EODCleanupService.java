package com.ihsmarkit.tfx.eod.service;

import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.ihsmarkit.tfx.core.dl.entity.eod.EodStatusCompositeId;
import com.ihsmarkit.tfx.core.dl.repository.TradeRepository;
import com.ihsmarkit.tfx.core.dl.repository.calendar.CalendarTradingSwapPointRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodDataRepository;
import com.ihsmarkit.tfx.core.dl.repository.eod.EodStatusRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.DailySettlementPriceRepository;
import com.ihsmarkit.tfx.core.dl.repository.marketdata.EodSwapPointRepository;
import com.ihsmarkit.tfx.core.domain.eod.EodStage;
import com.ihsmarkit.tfx.core.domain.type.TransactionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EODCleanupService {

    private final EodStatusRepository eodStatusRepository;

    private final TradeRepository tradeRepository;

    private final List<EodDataRepository> eodDataRepositories;

    private final JdbcTemplate jdbcTemplate;

    private final CalendarTradingSwapPointRepository calendarTradingSwapPointRepository;

    @Value("classpath*:/ledger/sql/eod_*_delete.sql")
    private Resource[] ledgersDeleteSQLs;

    @Value("classpath*:/ledger/sql/eod_ledger_monthly_trading_volume_delete.sql")
    private Resource monthlyLedgerDeleteSQL;

    void undoEODByDate(final LocalDate currentBusinessDate, final boolean keepMarketData) {
        log.info("[cleanup] removing all trades of type 2 for business date: {}", currentBusinessDate);
        tradeRepository.deleteAllByTransactionTypeAndTradeDate(TransactionType.BALANCE, currentBusinessDate);
        log.info("[cleanup] removing all data from EOD tables for business date: {}", currentBusinessDate);
        final List<EodDataRepository> repositories = eodDataRepositories.stream()
            .filter(repository -> !(keepMarketData && isMarketDataRepository(repository)))
            .collect(Collectors.toList());
        repositories.forEach(repository -> repository.deleteAllByDateGreaterThanEqual(currentBusinessDate));
        log.info("[cleanup] removing all data from ledgers tables for business date: {}", currentBusinessDate);
        deleteAllLedgersByBusinessDate(currentBusinessDate);
        removeEODStageRecordsForDate(currentBusinessDate);
    }

    private boolean isMarketDataRepository(final EodDataRepository repository) {
        return repository instanceof DailySettlementPriceRepository || repository instanceof EodSwapPointRepository;
    }

    private void removeEODStageRecordsForDate(final LocalDate businessDate) {
        Stream.of(
            EodStage.EOD1_COMPLETE,
            EodStage.EOD2_COMPLETE,
            EodStage.DSP_APPROVED,
            EodStage.SWAP_POINTS_APPROVED
        )
            .map(eodStage -> new EodStatusCompositeId(eodStage, businessDate))
            .forEach(this::deleteEodStatusIfExist);
    }

    private void deleteEodStatusIfExist(final EodStatusCompositeId eod2CompleteStatus) {
        if (eodStatusRepository.existsById(eod2CompleteStatus)) {
            eodStatusRepository.deleteById(eod2CompleteStatus);
        }
    }

    private void deleteAllLedgersByBusinessDate(final LocalDate businessDate) {
        Arrays.asList(ledgersDeleteSQLs).forEach(resource -> safeUpdate(businessDate, resource));
        calendarTradingSwapPointRepository.findMonthLastBusinessDate(YearMonth.from(businessDate))
            .filter(businessDate::equals)
            .map(date -> date.with(firstDayOfMonth()))
            .ifPresent(date -> safeUpdate(date, monthlyLedgerDeleteSQL));
    }

    private void safeUpdate(final LocalDate businessDate, final Resource resource) {
        try {
            jdbcTemplate.update(IOUtils.toString(resource.getInputStream()), businessDate.toString());
        } catch (final IOException ex) {
            log.warn("exception while deleting ledger using resource: {} on businessDate: {} with message: {}", resource, businessDate, ex.getMessage(), ex);
        }
    }

}
