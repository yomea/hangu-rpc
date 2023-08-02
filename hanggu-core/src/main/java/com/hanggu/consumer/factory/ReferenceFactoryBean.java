package com.hanggu.consumer.factory;

import com.hanggu.common.util.CommonUtils;
import com.hanggu.common.util.DescClassUtils;
import java.lang.reflect.Proxy;
import org.springframework.beans.factory.FactoryBean;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ReferenceFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceClass;

    @Override
    public T getObject() throws Exception {
        ClassLoader classLoader = CommonUtils.getClassLoader(ReferenceFactoryBean.class);
        // todo 实现类待写
        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[] {interfaceClass}, null);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }
}
