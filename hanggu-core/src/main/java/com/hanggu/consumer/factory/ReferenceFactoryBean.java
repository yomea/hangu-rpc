package com.hanggu.consumer.factory;

import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.annotation.HangguReference;
import com.hanggu.consumer.invocation.RpcReferenceHandler;
import java.lang.reflect.Proxy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ReferenceFactoryBean<T> implements FactoryBean<T> {

    private String groupName;

    private String version;

    private String interfaceName;
    private Class<T> interfaceClass;

    public ReferenceFactoryBean(String groupName, String interfaceName, String version, Class<T> interfaceClass) {
        this.groupName = groupName;
        if(!StringUtils.hasText(interfaceName)) {
            interfaceName = interfaceClass.getName();
        }
        this.interfaceName = interfaceName;
        this.version = version;
        this.interfaceClass = interfaceClass;
    }

    @Override
    public T getObject() throws Exception {
        ClassLoader classLoader = CommonUtils.getClassLoader(ReferenceFactoryBean.class);
        RpcReferenceHandler rpcReferenceHandler =
            new RpcReferenceHandler(this.groupName, this.interfaceName, this.version);

        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{interfaceClass}, rpcReferenceHandler);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
}
