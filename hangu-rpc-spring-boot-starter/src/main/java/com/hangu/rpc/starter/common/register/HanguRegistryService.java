package com.hangu.rpc.starter.common.register;

import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.entity.RegistryInfo;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.listener.RegistryNotifyListener;
import com.hangu.rpc.common.registry.AbstractRegistryService;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.hangu.center.common.entity.LookupServer;
import org.hangu.center.discover.client.DiscoverClient;

/**
 * @author wuzhenhong
 * @date 2023/8/17 10:14
 */
@Slf4j
public class HanguRegistryService extends AbstractRegistryService {

    private final DiscoverClient client;

    public HanguRegistryService(DiscoverClient client) {
        this.client = client;
    }

    @Override
    public void doRegister(RegistryInfo registryInfo) {
        client.register(this.toCenterRegistryInfo(registryInfo));
    }

    @Override
    public void unRegister(RegistryInfo serverInfo) {
        client.unRegister(this.toCenterRegistryInfo(serverInfo));
    }

    @Override
    public List<HostInfo> doSubscribe(RegistryNotifyListener listener, ServerInfo serverInfo) {

        // TODO：待完善
        return Collections.emptyList();
    }

    @Override
    public List<HostInfo> pullServers(ServerInfo serverInfo) {
        try {
            List<org.hangu.center.common.entity.RegistryInfo> registryInfos = client.lookup(this.toCenterLookServerInfo(serverInfo));
            return Optional.ofNullable(registryInfos).orElse(Collections.emptyList())
                .stream().map(e -> {
                    HostInfo hostInfo = new HostInfo();
                    hostInfo.setHost(e.getHostInfo().getHost());
                    hostInfo.setPort(e.getHostInfo().getPort());
                    return hostInfo;
                }).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doClose() {
//        client.close();
    }

    private org.hangu.center.common.entity.RegistryInfo toCenterRegistryInfo(RegistryInfo registryInfo) {
        org.hangu.center.common.entity.RegistryInfo info = new org.hangu.center.common.entity.RegistryInfo();
        info.setGroupName(registryInfo.getGroupName());
        info.setInterfaceName(registryInfo.getInterfaceName());
        info.setVersion(registryInfo.getVersion());
        HostInfo hostInfo = registryInfo.getHostInfo();
        if (Objects.nonNull(hostInfo)) {
            org.hangu.center.common.entity.HostInfo ipPort = new org.hangu.center.common.entity.HostInfo();
            ipPort.setHost(hostInfo.getHost());
            ipPort.setPort(hostInfo.getPort());
            info.setHostInfo(ipPort);
        }
        return info;
    }

    private LookupServer toCenterLookServerInfo(ServerInfo serverInfo) {
        LookupServer lookupServer = new LookupServer();
        lookupServer.setGroupName(serverInfo.getGroupName());
        lookupServer.setVersion(serverInfo.getVersion());
        lookupServer.setInterfaceName(serverInfo.getInterfaceName());
        return lookupServer;
    }
}
