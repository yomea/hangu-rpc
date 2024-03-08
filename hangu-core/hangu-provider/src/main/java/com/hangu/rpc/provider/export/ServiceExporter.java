package com.hangu.rpc.provider.export;

/**
 * @author wuzhenhong
 * @date 2023/8/17 15:16
 */
public class ServiceExporter {

    public static <T> void export(ServiceBean<T> serviceBean) {
        serviceBean.init();
    }

}
