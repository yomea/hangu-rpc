package com.hangu.provider.factory;

import com.hangu.common.entity.ServerInfo;
import com.hangu.common.registry.RegistryService;
import com.hangu.provider.export.ServiceBean;
import com.hangu.provider.export.ServiceExporter;
import com.hangu.provider.resolver.MethodArgumentResolverHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ServiceFactoryBean<T> implements FactoryBean<T>, InitializingBean {

    private ServerInfo serverInfo;

    private Class<T> interfaceClass;

    private T service;

    @Autowired
    private RegistryService registryService;

    @Autowired(required = false)
    private MethodArgumentResolverHandler methodArgumentResolverHandler;

    public ServiceFactoryBean(ServerInfo serverInfo,
        Class<T> interfaceClass, T service) {
        this.serverInfo = serverInfo;
        this.interfaceClass = interfaceClass;
        this.service = service;
    }

    @Override
    public T getObject() throws Exception {
        return service;
    }

    @Override
    public Class<?> getObjectType() {
        return this.interfaceClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        ServiceBean<T> serviceBean =
            new ServiceBean<>(serverInfo, interfaceClass, service, registryService, methodArgumentResolverHandler);
        ServiceExporter.export(serviceBean);
    }
}
