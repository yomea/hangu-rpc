package com.hanggu.provider.listener;

import com.hanggu.provider.annotation.HangguService;
import com.hanggu.provider.manager.LocalServiceManager;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/7/31 14:24
 */
public class ProviderApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        ApplicationContext applicationContext = event.getApplicationContext();
        Map<String, Object> beanNameMapServiceMap = applicationContext.getBeansWithAnnotation(HangguService.class);
        if (CollectionUtils.isEmpty(beanNameMapServiceMap)) {
            return;
        }
        beanNameMapServiceMap.forEach((beanName, service) -> {
            HangguService hangguService = AnnotationUtils.getAnnotation(service.getClass(), HangguService.class);
            String groupName = hangguService.groupName();
            String interfaceName = hangguService.interfaceName();
            String version = hangguService.version();
            // export service
            // step 1.0 本地暴露
            String key = Arrays.asList(groupName, interfaceName, version).stream()
                .filter(str -> Objects.nonNull(str) && !str.trim().isEmpty())
                .collect(Collectors.joining("/"));
            LocalServiceManager.register(key, service);
            // TODO: 2023/8/1 远程服务暴露
        });
    }
}
