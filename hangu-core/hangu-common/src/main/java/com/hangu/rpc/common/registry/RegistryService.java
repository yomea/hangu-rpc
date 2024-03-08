package com.hangu.rpc.common.registry;

import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.entity.RegistryInfo;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.listener.RegistryNotifyListener;
import java.util.List;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:40
 */
public interface RegistryService {

    void register(RegistryInfo registryInfo);

    void unRegister(RegistryInfo serverInfo);

    void subscribe(RegistryNotifyListener listener, ServerInfo serverInfo);

    List<HostInfo> pullServers(ServerInfo serverInfo);

    void retryRegister();

    void retrySubscribe();

    void close();
}
