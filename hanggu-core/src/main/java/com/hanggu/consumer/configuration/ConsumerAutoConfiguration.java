package com.hanggu.consumer.configuration;

import com.hanggu.consumer.properties.ConsumerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumerProperties.class)
public class ConsumerAutoConfiguration {

    @Autowired
    private ConsumerProperties consumerProperties;
}
