package com.hanggu.consumer.listener;

import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.properties.HanguProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:08
 */
public class ConsumerApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private HanguProperties hanguProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 启动netty客户端
        HanguRpcManager.openClient(hanguProperties);
    }
}
