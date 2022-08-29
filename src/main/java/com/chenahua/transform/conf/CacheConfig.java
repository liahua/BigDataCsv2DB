package com.chenahua.transform.conf;

import com.chenahua.transform.service.CacheService;
import com.github.benmanes.caffeine.cache.*;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.concurrent.*;

@Configuration
@Slf4j
@EnableCaching
public class CacheConfig {
    public static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setNameFormat("cache-%d").setUncaughtExceptionHandler((t, e) -> log.error("异常", e)).build();
    private final CacheService cacheService;

    public CacheConfig(CacheService cacheService) {
        this.cacheService = cacheService;
    }


    private @NonNull LoadingCache<Object, Object> loadingCache(CacheLoader<Object, Object> cacheLoader) {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(5, THREAD_FACTORY)))
                .maximumSize(1000)
                .recordStats()
                .build(cacheLoader);
    }

    @Bean("basicCache")
    public CacheManager cacheManager(CacheLoader<Object, Object> cacheLoader) {

        LoadingCache<Object, Object> build = loadingCache(cacheLoader);
        CaffeineCache cache = new CaffeineCache("basic_cache", build);

        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(Collections.singleton(cache));
        return simpleCacheManager;
    }


    @Bean
    public CacheLoader<Object, Object> cacheLoader() {
        return new CacheLoader<Object, Object>() {
            @Override
            public @Nullable Object load(@NonNull Object key) throws Exception {
                log.info("load key =" + key);
                String s = cacheService.selectAdapter(key.toString());
                log.info("load key value =" + key + "|" + s);
                return s;
            }

            @Override
            public @Nullable Object reload(@NonNull Object key, @NonNull Object oldValue) throws Exception {
                System.out.println("sleep key = " + key + ", oldValue = " + oldValue);
                Thread.sleep(10000);
                log.info("refreshAfterWrite");
                return CacheLoader.super.reload(key, oldValue);
            }
        };
    }

}
