package com.hangu.common.registry;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.hangu.common.entity.HostInfo;
import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.RegistryNotifyInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.listener.RegistryNotifyListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuzhenhong
 * @date 2023/8/18 9:11
 */
public abstract class AbstractRegistryService implements RegistryService{

    protected final Set<RegistryInfo> registered = new ConcurrentHashSet<>();
    protected final Map<ServerInfo, RegistryNotifyListener> subscriberMap = new ConcurrentHashMap<>();

    @Override
    public void register(RegistryInfo registryInfo) {
        this.doRegister(registryInfo);
        registered.add(registryInfo);
    }

    protected abstract void doRegister(RegistryInfo registryInfo);

    @Override
    public void subscribe(RegistryNotifyListener listener, ServerInfo serverInfo) {
        List<HostInfo> hostInfoList = this.doSubscribe(listener, serverInfo);
        RegistryNotifyInfo notifyInfo = new RegistryNotifyInfo();
        notifyInfo.setServerInfo(serverInfo);
        notifyInfo.setHostInfos(hostInfoList);
        listener.registryNotify(notifyInfo);
        subscriberMap.put(serverInfo, listener);
    }

    protected abstract List<HostInfo> doSubscribe(RegistryNotifyListener listener, ServerInfo serverInfo);

    @Override
    public void retryRegister() {
        registered.stream().forEach(this::register);
    }

    @Override
    public void retrySubscribe() {
        subscriberMap.forEach(((serverInfo, registryNotifyListener) -> {
            this.subscribe(registryNotifyListener, serverInfo);
        }));
    }
}
