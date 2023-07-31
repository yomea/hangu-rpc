package com.hanggu.consumer.listener;

import com.hanggu.consumer.annotation.HangguReference;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/7/31 14:24
 */
public class ConsumerApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        ApplicationContext applicationContext = event.getApplicationContext();
        Map<String, Object> beanNameMapServiceMap = applicationContext.getBeansWithAnnotation(HangguReference.class);
        if(CollectionUtils.isEmpty(beanNameMapServiceMap)) {
            return;
        }
        beanNameMapServiceMap.forEach((beanName, service) -> {
            // export service
        });
    }
}
