package com.hanggu.consumer.configuration;

import com.hanggu.common.constant.HangguCons;
import com.hanggu.consumer.properties.ConsumerProperties;
import com.hanggu.provider.listener.ProviderApplicationListener;
import com.hanggu.provider.properties.ProviderProperties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ConsumerProperties.class)
public class ConsumerAutoConfiguration {

    @Autowired
    private ConsumerProperties consumerProperties;

    @Bean
    public ProviderApplicationListener providerApplicationListener() {
        return new ProviderApplicationListener();
    }

}
