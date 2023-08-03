package com.hanggu.consumer.configuration;

import com.hanggu.consumer.listener.ConsumerApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:21
 */
@Configuration(proxyBeanMethods = false)
public class ConsumerConfiguration {

    @Bean
    public ConsumerApplicationListener consumerApplicationListener() {
        return new ConsumerApplicationListener();
    }

}
