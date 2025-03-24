package com.hangu.rpc.common.registry;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.entity.RegistryInfo;
import com.hangu.rpc.common.entity.RegistryNotifyInfo;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.listener.RegistryNotifyListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuzhenhong
 * @date 2023/8/18 9:11
 */
public abstract class AbstractRegistryService implements RegistryService {

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
        subscriberMap.put(serverInfo, listener);
        List<HostInfo> hostInfoList = this.doSubscribe(listener, serverInfo);
        RegistryNotifyInfo notifyInfo = new RegistryNotifyInfo();
        notifyInfo.setServerInfo(serverInfo);
        notifyInfo.setHostInfos(hostInfoList);
        listener.registryNotify(notifyInfo);
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

    @Override
    public void close() {
        registered.stream().forEach(this::unRegister);
        registered.clear();
        this.doClose();
    }

    protected abstract void doClose();
}
