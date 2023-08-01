package com.hanggu.provider.listener;

import com.hanggu.provider.annotation.HangguService;
import com.hanggu.provider.invoker.RpcInvoker;
import com.hanggu.provider.manager.LocalServiceManager;
import com.hanggu.provider.properties.ProviderProperties;
import com.hanggu.provider.server.NettyServerBoostrap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2023/7/31 14:24
 */
public class ProviderApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private Executor rpcInvokerExecutor;

    @Autowired
    private ProviderProperties providerProperties;

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
            List<String> interfaceNameList = new ArrayList<>();
            if(Objects.nonNull(interfaceName) && !interfaceName.trim().isEmpty()) {
                Class<?>[] interfaces = service.getClass().getInterfaces();
                if(Objects.isNull(interfaces) || interfaces.length == 0) {
                    interfaceNameList.add(service.getClass().getName());
                } else {
                    Arrays.stream(interfaces).forEach(i -> {
                        interfaceNameList.add(i.getName());
                    });
                }
            } else {
                interfaceNameList.add(interfaceName);
            }
            String version = hangguService.version();
            String prefixKey = groupName + "/" + version;
            RpcInvoker rpcInvoker = new RpcInvoker();
            rpcInvoker.setService(service);
            interfaceNameList.stream().forEach(intName ->{
                String key = prefixKey + "/" + intName;
                // export service
                // step 1.0 本地暴露
                LocalServiceManager.register(key, rpcInvoker);

                // TODO: 2023/8/1 远程服务暴露
            });
        });

        NettyServerBoostrap nettyServerBoostrap = new NettyServerBoostrap();
        nettyServerBoostrap.start(providerProperties, rpcInvokerExecutor);
    }
}
