package com.chenahua.transform.conf;

import cn.hutool.extra.spring.SpringUtil;
import com.chenahua.transform.service.CacheService;
import com.github.benmanes.caffeine.cache.*;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    @Bean
    public AsyncCache<String, String> initCache(CacheLoader<String, String> cacheLoader) {
        ExecutorService executor = Executors.newWorkStealingPool(20);
        return Caffeine.newBuilder()
                .expireAfter(new Expiry<String, String>() {
                    @Override
                    public long expireAfterCreate(@NonNull String key, @NonNull String value, long currentTime) {
                        if (value.contains("temp")) {
                            return TimeUnit.SECONDS.toNanos(30);
                        }
                        return TimeUnit.MINUTES.toNanos(2);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull String key, @NonNull String value, long currentTime, @NonNegative long currentDuration) {
                        return expireAfterCreate(key, value, currentTime);
                    }

                    @Override
                    public long expireAfterRead(@NonNull String key, @NonNull String value, long currentTime, @NonNegative long currentDuration) {
                        return currentDuration;
                    }
                })
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(5, THREAD_FACTORY)))
                .maximumSize(1000)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.info("自动移除key {}, value {} ,cause {}", key, value, cause);
                    if (RemovalCause.EXPIRED.equals(cause)) {
                        log.info("由于key过期导致,重新进行加载,key:{}", key);
                        AsyncCache<String, String> cache = SpringUtil.getBean(AsyncCache.class);
                        cache.put(key, cacheLoader.asyncLoad(key, executor));
                    }
                })
                .executor(executor)
                .buildAsync(cacheLoader);
    }


    private @NonNull LoadingCache<Object, Object> loadingCache(CacheLoader<Object, Object> cacheLoader) {
        return Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .refreshAfterWrite(10, TimeUnit.SECONDS)
                .scheduler(Scheduler.forScheduledExecutorService(new ScheduledThreadPoolExecutor(5, THREAD_FACTORY)))
                .maximumSize(1000)
                .recordStats()
                .removalListener((key, value, cause) -> {
                    log.info("移除key {} ,value {} , cause {}", key, value, cause);
                })
                .build(cacheLoader);
    }

    //    @Bean("basicCache")
    public CacheManager cacheManager(CacheLoader<Object, Object> cacheLoader) {

        LoadingCache<Object, Object> build = loadingCache(cacheLoader);
        CaffeineCache cache = new CaffeineCache("basic_cache", build);

        SimpleCacheManager simpleCacheManager = new SimpleCacheManager();
        simpleCacheManager.setCaches(Collections.singleton(cache));
        return simpleCacheManager;
    }


    @Bean
    public CacheLoader<String, String> cacheLoader() {
        return new CacheLoader<String, String>() {
            @Override
            public @Nullable String load(@NonNull String key) {
                log.info("缓存载入中" + key);
                String s = cacheService.selectAdapter(key);
                log.info("缓存载入完毕" + key + "|" + s);
                return s;
            }
        };
    }

}
