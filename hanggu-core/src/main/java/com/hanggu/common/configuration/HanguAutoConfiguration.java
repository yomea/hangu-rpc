package com.hanggu.common.configuration;

import com.hanggu.common.constant.HangguCons;
import com.hanggu.common.properties.HanguProperties;
import com.hanggu.consumer.configuration.ConsumerConfiguration;
import com.hanggu.consumer.properties.ConsumerProperties;
import com.hanggu.provider.configuration.ProviderConfiguration;
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
import org.springframework.context.annotation.Import;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HanguProperties.class)
@Import({ProviderConfiguration.class, ConsumerConfiguration.class})
public class HanguAutoConfiguration {

    @Autowired
    private HanguProperties hanguProperties;

    @Bean
    public Executor rpcInvokerExecutor() {

        int coreNum = hanguProperties.getCoreNum();
        coreNum = coreNum <= 0 ? HangguCons.DEF_IO_THREADS * 4 : coreNum;
        int maxNum = hanguProperties.getMaxNum();
        maxNum = maxNum <= 0 ? HangguCons.CPUS * 8 : maxNum;

        return new ThreadPoolExecutor(coreNum, maxNum,
            10L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000));
    }
}