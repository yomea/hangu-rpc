package com.hangu.common.listener;

import com.hangu.common.entity.RegistryNotifyInfo;

/**
 * 注册中心通知
 *
 * @author wuzhenhong
 * @date 2023/8/2 17:20
 */
public interface RegistryNotifyListener {

    void registryNotify(RegistryNotifyInfo notifyInfo);

}
