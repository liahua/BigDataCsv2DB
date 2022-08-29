package com.chenahua.transform.service.impl;

import com.chenahua.transform.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {
    @Override
    @Cacheable(value = "basic_cache",key = "#o",cacheManager = "basicCache")
    public String selectNumber(String o) {
        String s = o + ":" + System.currentTimeMillis();
        log.info(s);
        return s;
    }

    @Override
    public String selectAdapter(String o) {
        try {
            log.info("查库开始");
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String s = selectNumber(o);
        log.info("查库结束");
        return s;
    }
}
