package com.hanggu.consumer.manager;

import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.entity.RegistryNotifyInfo;
import com.hanggu.common.entity.ServerInfo;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.registry.RegistryService;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.client.ClientConnect;
import com.hanggu.consumer.client.NettyClient;
import com.hanggu.consumer.listener.RegistryNotifyListener;
import io.netty.channel.Channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:56
 */
@Slf4j
public class ConnectManager implements RegistryNotifyListener {

    private static final Map<String, ClientConnect> GLOBAL_HOST_INFO_CHANNEL = new ConcurrentHashMap<>();

    private List<ClientConnect> KEY_CHANNELS = new ArrayList<>();
    private final Map<String, Object> KEY_LOCK = new ConcurrentHashMap<>(8192);

    private ServerInfo serverInfo;

    private RegistryService registryService;

    public ConnectManager(RegistryService registryService, ServerInfo serverInfo) {
        this.registryService = registryService;
        this.serverInfo = serverInfo;
        // 初始化从远程拉取服务
        this.initPullService();
        // 向注册中心订阅该服务的列表变化情况
        this.subscribe(serverInfo);
    }

    @Override
    public void registryNotify(RegistryNotifyInfo notifyInfo) {
        // 刷新本地服务列表
        this.cacheConnect(notifyInfo.getHostInfos());

    }

    private void cacheConnect(List<HostInfo> hostInfoList) {
        hostInfoList.stream().forEach(this::doCacheConnect);
    }

    private boolean hitCache(String key) {

        ClientConnect clientConnect = GLOBAL_HOST_INFO_CHANNEL.get(key);
        if (Objects.nonNull(clientConnect)) {
            Channel channel = clientConnect.getChannel();
            if(channel.isActive()) {
                KEY_CHANNELS.add(clientConnect);
                return true;
            } else {
                GLOBAL_HOST_INFO_CHANNEL.remove(key);
            }
        }
        return false;
    }

    private void doCacheConnect(HostInfo hostInfo) {
        // 共享连接（netty的write方法做了线程安全处理，对于外部线程提交的数据，会包装成task加入到队列中，使用netty线程写出）
        String key = hostInfo.getHost() + ":" + hostInfo.getPort();
        if(this.hitCache(key)) {
            return;
        }
        KEY_LOCK.putIfAbsent(key, new Object());
        try {
            synchronized (KEY_LOCK.get(key)) {
                if(this.hitCache(key)) {
                    return;
                }

                NettyClient nettyClient = HanguRpcManager.getNettyClient();
                Channel channel;
                try {
                    channel = nettyClient.syncConnect(hostInfo.getHost(), hostInfo.getPort());
                } catch (InterruptedException e) {
                    log.error("连接失败，跳过");
                    throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "连接失败，跳过", e);
                }
                ClientConnect client = new ClientConnect();
                client.setChannel(channel);
                client.setHostInfo(hostInfo);
                KEY_CHANNELS.add(client);
                GLOBAL_HOST_INFO_CHANNEL.put(key, client);
            }
        } finally {
            KEY_LOCK.remove(key);
        }
    }

    public List<ClientConnect> getConnects() {
        return KEY_CHANNELS.stream()
                .filter(connect -> Objects.nonNull(connect.getChannel()) && connect.getChannel().isActive())
                .collect(Collectors.toList());
    }

    private void subscribe(ServerInfo serverInfo) {
        this.registryService.subscribe(this, serverInfo);
    }

    private void initPullService() {
        List<HostInfo> infos = registryService.pullServers(serverInfo);
        this.cacheConnect(infos);
    }
}
