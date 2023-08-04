package com.hanggu.common.configuration;

import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.properties.HanguProperties;
import com.hanggu.consumer.configuration.ConsumerConfiguration;
import com.hanggu.provider.configuration.ProviderConfiguration;
import java.util.concurrent.Executor;
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
    public Executor rpcIoExecutor() {
        return HanguRpcManager.openIoExecutor(hanguProperties);
    }
}
