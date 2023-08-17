package com.hanggu.consumer.configuration;

import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.consumer.manager.RpcRequestManager;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Configuration;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:21
 */
@Configuration(proxyBeanMethods = false)
public class ConsumerConfiguration implements DisposableBean {

    @Override
    public void destroy() throws Exception {
        HanguRpcManager.closeClient();
    }
}
