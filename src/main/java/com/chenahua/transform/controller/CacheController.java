package com.chenahua.transform.controller;

import com.chenahua.transform.service.CacheService;
import com.chenahua.transform.service.impl.CacheServiceImpl;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/cache")
public class CacheController {

    @Autowired
    private CacheService cacheService;
    @Autowired
    private AsyncCache<String, String> cacheBasic;
    @Autowired
    private CacheLoader<String,String> cacheLoader;


    @RequestMapping("/test/{key}")
    public List<String> randomCache(@PathVariable("key") String key) {
        String[] strs = new String[]{"aaa", "bbb", "c", "q", "a", "z", "w", "e", "r", "t", "y", "z", "x", "v", "n","tempA","tempB","tempC"};
        List<String> keys = Arrays.asList(strs);
        return keys.parallelStream().map(obj -> {
            CompletableFuture<String> future = cacheBasic.get(obj, s -> {
                try {
                    return cacheLoader.load(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            });
            return future.getNow("无数据");
        }).collect(Collectors.toList());


    }

    @RequestMapping("/test1/{key}")
    public String randomCache2(@PathVariable("key") String key) {
        return cacheService.selectNumber(key);
    }


}
