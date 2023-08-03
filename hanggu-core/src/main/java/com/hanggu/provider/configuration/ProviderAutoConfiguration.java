package com.hanggu.provider.configuration;

import com.hanggu.common.constant.HangguCons;
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
@EnableConfigurationProperties(ProviderProperties.class)
public class ProviderAutoConfiguration {

    @Autowired
    private ProviderProperties providerProperties;

    @Bean
    public ProviderApplicationListener providerApplicationListener() {
        return new ProviderApplicationListener();
    }

    @Bean
    public Executor rpcInvokerExecutor() {

        int coreNum = providerProperties.getCoreNum();
        coreNum = coreNum <= 0 ? HangguCons.DEF_IO_THREADS * 4 : coreNum;
        int maxNum = providerProperties.getMaxNum();
        maxNum = maxNum <= 0 ? HangguCons.CPUS * 8 : maxNum;

        return new ThreadPoolExecutor(coreNum, maxNum,
            10L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10000));
    }

}
