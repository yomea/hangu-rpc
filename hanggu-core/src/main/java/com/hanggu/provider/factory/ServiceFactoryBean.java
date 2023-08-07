package com.hanggu.provider.factory;

import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.registry.RegistryService;
import com.hanggu.provider.invoker.RpcInvoker;
import com.hanggu.provider.manager.LocalServiceManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ServiceFactoryBean<T> implements FactoryBean<T>, InitializingBean {

    private String groupName;

    private String interfaceName;

    private String version;

    private Class<T> interfaceClass;

    private T service;

    @Autowired
    private RegistryService registryService;

    public ServiceFactoryBean(String groupName, String interfaceName, String version,
        Class<T> interfaceClass, T service) {
        this.groupName = groupName;
        this.interfaceName = interfaceName;
        this.version = version;
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

        String key = groupName + "/" + version + "/" + interfaceName;
        RpcInvoker rpcInvoker = new RpcInvoker();
        rpcInvoker.setService(service);
        // export service
        // step 1.0 本地暴露
        LocalServiceManager.register(key, rpcInvoker);

        // step 2.0 远程暴露
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setGroupName(groupName);
        registryInfo.setInterfaceName(interfaceName);
        registryInfo.setVersion(version);
        registryInfo.setHostInfo(HanguRpcManager.getLocalHost());
        registryService.register(registryInfo);
    }
}
