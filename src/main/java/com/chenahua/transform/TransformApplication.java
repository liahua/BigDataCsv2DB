package com.chenahua.transform;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

@Order
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@Import(SpringUtil.class)
public class TransformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransformApplication.class, args);
    }

    @Bean
    ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }

}
