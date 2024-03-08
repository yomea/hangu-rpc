package com.hangu.rpc.consumer.reference;

import com.hangu.rpc.common.util.CommonUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/17 15:18
 */
public class ServiceReference {

    public static <T> T reference(ReferenceBean<T> referenceBean) {
        return reference(referenceBean, CommonUtils
            .getClassLoader(referenceBean.getClass()));
    }

    public static <T> T reference(ReferenceBean<T> referenceBean, ClassLoader classLoader) {
        // 初始化
        referenceBean.init();
        // 构建服务代理
        return referenceBean.buildServiceProxy(classLoader);
    }
}
