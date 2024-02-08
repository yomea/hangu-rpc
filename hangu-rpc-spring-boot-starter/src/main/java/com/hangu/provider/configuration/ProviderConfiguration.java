package com.hangu.provider.configuration;

import com.hangu.provider.listener.ProviderApplicationListener;
import com.hangu.provider.resolver.MethodArgumentResolverHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
public class ProviderConfiguration {

    @Bean
    public ProviderApplicationListener providerApplicationListener() {
        return new ProviderApplicationListener();
    }

    @Bean
    public MethodArgumentResolverHandler methodArgumentResolverHandler() {
        return new MethodArgumentResolverHandler();
    }
}
