package com.hangu.rpc.starter.consumer.factory;

import com.hangu.rpc.common.entity.RequestHandlerInfo;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.properties.ExecutorProperties;
import com.hangu.rpc.common.properties.HanguProperties;
import com.hangu.rpc.common.registry.RegistryService;
import com.hangu.rpc.common.util.CommonUtils;
import com.hangu.rpc.consumer.reference.ReferenceBean;
import com.hangu.rpc.consumer.reference.ServiceReference;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ReferenceFactoryBean<T> implements FactoryBean<T>, InitializingBean {

    private ServerInfo serverInfo;

    private Class<T> interfaceClass;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private HanguProperties hanguProperties;

    private T service;

    public ReferenceFactoryBean(ServerInfo serverInfo, Class<T> interfaceClass) {
        this.serverInfo = serverInfo;
        this.interfaceClass = interfaceClass;
    }

    @Override
    public T getObject() throws Exception {
        return this.service;
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RequestHandlerInfo requestHandlerInfo = new RequestHandlerInfo();
        requestHandlerInfo.setServerInfo(this.serverInfo);
        ExecutorProperties executorProperties = new ExecutorProperties();
        executorProperties.setCoreNum(hanguProperties.getCoreNum());
        executorProperties.setMaxNum(hanguProperties.getMaxNum());
        ReferenceBean<T> referenceBean = new ReferenceBean<>(requestHandlerInfo, this.interfaceClass, registryService,
            executorProperties);
        this.service = ServiceReference.reference(referenceBean, CommonUtils.getClassLoader(this.getClass()));
    }
}
