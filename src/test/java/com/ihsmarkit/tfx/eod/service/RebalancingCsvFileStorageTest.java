package com.ihsmarkit.tfx.eod.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;

class RebalancingCsvFileStorageTest {

    private Path baseLedgersPath;
    private RebalancingCsvFileStorage storage;

    @BeforeEach
    @SneakyThrows
    void initStorage() {
        this.baseLedgersPath = Files.createTempDirectory(null);
        this.storage = new RebalancingCsvFileStorage(baseLedgersPath.toAbsolutePath().toString());
    }

    @Test
    @SneakyThrows
    void shouldStoreFileInLedgerSpecificDirectory() {
        final var fileContent1 = "content_1".getBytes(StandardCharsets.UTF_8);
        final var fileContent2 = "content_2".getBytes(StandardCharsets.UTF_8);

        storage.store(LocalDate.of(2222, 3, 4), Map.of(
            "LP01", fileContent1,
            "LP02", fileContent2
        ));

        assertThat(baseLedgersPath.resolve("2222-03-04/LP01.csv"))
            .exists()
            .hasBinaryContent(fileContent1);
        assertThat(baseLedgersPath.resolve("2222-03-04/LP02.csv"))
            .exists()
            .hasBinaryContent(fileContent2);
    }

}