package com.chenahua.transform.controller;

import com.chenahua.transform.service.CacheService;
import com.chenahua.transform.service.impl.CacheServiceImpl;
import com.github.benmanes.caffeine.cache.AsyncCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;
//    @Autowired
    private AsyncCache<String, String> cacheBasic;

    @RequestMapping("/test/{key}")
    public String randomCache(@PathVariable("key") String key) {
        CompletableFuture<String> future = cacheBasic.getIfPresent(key);
        if (future == null) {
            String s = cacheService.selectNumber(key);
            log.info("future为null,手动放入");
            cacheBasic.put(key, CompletableFuture.completedFuture(s));
            return s;
        }

        return future.getNow("无数据");
    }

    @RequestMapping("/test1/{key}")
    public String randomCache2(@PathVariable("key") String key) {
        return cacheService.selectNumber(key);
    }


}
