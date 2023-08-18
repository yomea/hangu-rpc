package com.hangu.common.registry;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.listener.RegistryNotifyListener;
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
