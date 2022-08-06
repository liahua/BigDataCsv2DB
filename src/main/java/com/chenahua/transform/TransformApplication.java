package com.chenahua.transform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.Order;

@Order
@SpringBootApplication
public class TransformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransformApplication.class, args);
    }

}
