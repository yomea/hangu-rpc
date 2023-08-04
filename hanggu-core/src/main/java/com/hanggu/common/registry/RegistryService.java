package com.hanggu.common.registry;

import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.entity.ServerInfo;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:40
 */
public interface RegistryService {

    void register(RegistryInfo registryInfo);

    void unRegister(RegistryInfo registryInfo);

    void subscribe(RegistryInfo registryInfo);

    void unSubscribe(RegistryInfo registryInfo);

    List<HostInfo> pullServers(ServerInfo serverInfo);

}
