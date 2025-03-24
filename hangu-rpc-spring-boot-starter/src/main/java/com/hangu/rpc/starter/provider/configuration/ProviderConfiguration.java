package com.hangu.rpc.starter.provider.configuration;

import com.hangu.rpc.provider.resolver.MethodArgumentResolver;
import com.hangu.rpc.provider.resolver.MethodArgumentResolverHandler;
import com.hangu.rpc.starter.provider.listener.ProviderApplicationListener;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;

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
    public ConversionService customerConversionService() {
        DateTimeFormatters dateTimeFormatters = new DateTimeFormatters()
            .dateTimeFormat("yyyy-MM-dd HH:mm:ss")
            .dateFormat("yyyy-MM-dd")
            .timeFormat("HH:mm:ss");
        ConfigurableConversionService conversionService = new WebConversionService(dateTimeFormatters);
//        conversionService.addConverter(new StringToCharConverter());
        return conversionService;
    }

    @Bean
    public MethodArgumentResolverHandler methodArgumentResolverHandler(
        @Autowired(required = false) ConversionService customerConversionService,
        @Autowired(required = false) List<MethodArgumentResolver> resolverList) {
        // conversionService 可自定义
        MethodArgumentResolverHandler resolverHandler = new MethodArgumentResolverHandler(customerConversionService);
//        如果当前满足不了你的参数解析需求，可以自己扩展，然后可以自己加排序
        resolverHandler.addCustomerResolvers(resolverList);
        return resolverHandler;
    }
}
