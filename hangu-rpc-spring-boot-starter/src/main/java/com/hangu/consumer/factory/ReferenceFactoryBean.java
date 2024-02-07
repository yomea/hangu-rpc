package com.hangu.consumer.factory;

import com.hangu.common.entity.RequestHandlerInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.properties.HanguProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import com.hangu.consumer.reference.ReferenceBean;
import com.hangu.consumer.reference.ServiceReference;
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
        requestHandlerInfo.setHttp(false);
        requestHandlerInfo.setServerInfo(this.serverInfo);
        ReferenceBean<T> referenceBean = new ReferenceBean<>(requestHandlerInfo, this.interfaceClass, registryService,
            hanguProperties);
        this.service = ServiceReference.reference(referenceBean, CommonUtils.getClassLoader(this.getClass()));
    }
}
