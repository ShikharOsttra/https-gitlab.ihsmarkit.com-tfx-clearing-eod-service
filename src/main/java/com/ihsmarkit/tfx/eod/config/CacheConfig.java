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

    public static final String TRADE_DATES_CACHE = "tradeDates";
    public static final String VALUE_DATES_CACHE = "valueDates";

    @Bean
    @JobScope
    public CacheManager jobCacheManager() {
        return new ConcurrentMapCacheManager(TRADE_DATES_CACHE, VALUE_DATES_CACHE);
    }

    @Bean
    @JobScope
    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(jobCacheManager());
    }

}
