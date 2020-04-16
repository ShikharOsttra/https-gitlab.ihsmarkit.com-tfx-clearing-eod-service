package com.ihsmarkit.tfx.eod.config;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@Configuration
public class CacheConfig extends CachingConfigurerSupport {

    public static final String NEXT_TRADE_DATES_CACHE = "nextTradeDates";
    public static final String PREVIOUS_TRADE_DATES_CACHE = "previousTradeDates";
    public static final String IS_TRADABLE = "isTradable";
    public static final String VALUE_DATES_CACHE = "valueDates";
    public static final String VM_SETTLEMENT_DATES_CACHE = "vmSettlementDates";
    public static final String JPY_RATES_CACHE = "joyRatesCache";
    public static final String JPY_CROSS_RATES_CACHE = "joyCrossRatesCache";


    @Bean
    @JobScope
    public CacheManager jobCacheManager() {
        return new ConcurrentMapCacheManager(
            NEXT_TRADE_DATES_CACHE,
            PREVIOUS_TRADE_DATES_CACHE,
            IS_TRADABLE,
            VALUE_DATES_CACHE,
            VM_SETTLEMENT_DATES_CACHE,
            JPY_RATES_CACHE,
            JPY_CROSS_RATES_CACHE
        );
    }

    @Bean
    @JobScope
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(jobCacheManager());
    }

}
