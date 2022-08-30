package com.chenahua.transform.service.impl;

import com.chenahua.transform.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CacheServiceImpl implements CacheService {
    @Override
    public String selectNumber(String o) {
        return o + ":" + System.currentTimeMillis();
    }

    @Override
    public String selectAdapter(String o) {
        try {
            log.info("模拟查库 ,key {}",o);
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String s = selectNumber(o);
        log.info("模拟查库结束,key {} value{}",o,s);
        return s;
    }
}
