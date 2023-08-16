package com.hanggu.consumer.listener;

import com.hanggu.common.entity.RegistryNotifyInfo;

/**
 * 注册中心通知
 *
 * @author wuzhenhong
 * @date 2023/8/2 17:20
 */
public interface RegistryNotifyListener {

    void registryNotify(RegistryNotifyInfo notifyInfo);

}
