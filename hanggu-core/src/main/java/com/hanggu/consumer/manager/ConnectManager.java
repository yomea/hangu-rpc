package com.hanggu.consumer.manager;

import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.entity.RegistryNotifyInfo;
import com.hanggu.common.entity.ServerInfo;
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

    private final Map<String, List<ClientConnect>> KEY_CHANNELS = new ConcurrentHashMap<>(8192);
    private final Map<String, Object> KEY_LOCK = new ConcurrentHashMap<>(8192);
    private static final Object OBJECT = new Object();

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
        String key = CommonUtils.createServiceKey(notifyInfo.getServerInfo());
        this.cacheConnects(key, notifyInfo.getHostInfos());

    }


    private void cacheConnects(String key, List<HostInfo> hostInfoList) {
        if (CollectionUtils.isEmpty(hostInfoList)) {
            return;
        }
        // 防御性操作，正常情况下没有并发操作（每隔接口会单独维护自己的服务列表）
        while (Objects.nonNull(KEY_LOCK.putIfAbsent(key, OBJECT))) {
            // 自旋
            Thread.yield();
        }
        try {
            List<ClientConnect> connectList = KEY_CHANNELS.getOrDefault(key, new ArrayList<>());
            // 获取有效的链接
            Set<HostInfo> hostInfoSet = connectList.stream().filter(con -> con.getChannel().isActive())
                .map(ClientConnect::getHostInfo).collect(Collectors.toSet());
            hostInfoList = hostInfoList.stream().filter(hostInfo -> !hostInfoSet.contains(hostInfo))
                .collect(Collectors.toList());
            NettyClient nettyClient = HanguRpcManager.getNettyClient();
            List<ClientConnect> clients = hostInfoList.stream().map(hostInfo -> {
                // 注意，这样直接链接的是异步的，获取链接的时候还是需要检查是否链接成功，方可使用
                Channel channel = null;
                try {
                    channel = nettyClient.syncConnect(hostInfo.getHost(), hostInfo.getPort());
                } catch (InterruptedException e) {
                    log.error("连接失败，跳过");
                }
                ClientConnect client = new ClientConnect();
                client.setChannel(channel);
                client.setHostInfo(hostInfo);
                return client;
            }).collect(Collectors.toList());

            if (CollectionUtils.isEmpty(clients)) {
                return;
            }

            connectList.addAll(clients);

            KEY_CHANNELS.put(key, connectList);
        } finally {
            KEY_LOCK.remove(key);
        }
    }

    public List<ClientConnect> getConnects(String key) {
        List<ClientConnect> connects = KEY_CHANNELS.getOrDefault(key, Collections.emptyList());
        return connects.stream()
            .filter(connect -> Objects.nonNull(connect.getChannel()) && connect.getChannel().isActive())
            .collect(Collectors.toList());
    }

    private void subscribe(ServerInfo serverInfo) {
        this.registryService.subscribe(this, serverInfo);
    }

    private void initPullService() {
        List<HostInfo> infos = registryService.pullServers(serverInfo);
        String key = CommonUtils.createServiceKey(serverInfo.getGroupName(), serverInfo.getInterfaceName(),
            serverInfo.getVersion());
        this.cacheConnects(key, infos);
    }
}
