package com.hanggu.provider.registry;

import com.hanggu.common.entity.RegistryInfo;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:40
 */
public interface RegistryService {

    void register(RegistryInfo registryInfo);

    void unRegister(RegistryInfo registryInfo);

    void subscribe(RegistryInfo registryInfo);

    void unSubscribe(RegistryInfo registryInfo);

}
