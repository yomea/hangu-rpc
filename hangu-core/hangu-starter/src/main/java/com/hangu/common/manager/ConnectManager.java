package com.hangu.common.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.hangu.common.entity.HostInfo;
import com.hangu.common.entity.RegistryNotifyInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.exception.RpcInvokerException;
import com.hangu.common.listener.RegistryNotifyListener;
import com.hangu.common.registry.RegistryService;
import com.hangu.consumer.client.ClientConnect;
import com.hangu.consumer.client.NettyClient;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:56
 */
@Slf4j
public class ConnectManager implements RegistryNotifyListener {

    private static final Map<String, ClientConnect> GLOBAL_HOST_INFO_CHANNEL = new ConcurrentHashMap<>();

    private List<ClientConnect> KEY_CHANNELS = Collections.emptyList();

    private ServerInfo serverInfo;

    private RegistryService registryService;

    private volatile ScheduledFuture<?> future;

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

    /**
     * 每个接口都有一个由自己管理的链接管理器
     * 正常情况下是没有并发的，不过这里做个预防性编程
     *
     * @param hostInfoList
     */
    private synchronized void cacheConnect(List<HostInfo> hostInfoList) {
        // 筛选出活着的通道
        List<ClientConnect> activeChannelList = this.KEY_CHANNELS.stream().filter(e -> e.getChannel().isActive())
            .collect(Collectors.toList());
        Set<HostInfo> exitsActiveHostMap = activeChannelList.stream()
            .map(e -> e.getHostInfo())
            .collect(Collectors.toSet());
        hostInfoList = hostInfoList.stream().filter(e -> !exitsActiveHostMap.contains(e)).collect(Collectors.toList());
        if (CollectionUtil.isEmpty(hostInfoList)) {
            // 表示都已经缓存过了，无效新增
            if (this.KEY_CHANNELS.size() != activeChannelList.size()) {
                this.KEY_CHANNELS = activeChannelList;
            }
            return;
        }
        List<ClientConnect> newConnectList = hostInfoList.stream().map(this::doCacheConnect)
            .collect(Collectors.toList());
        activeChannelList.addAll(newConnectList);
        this.KEY_CHANNELS = activeChannelList;
    }

    private ClientConnect hitCache(String key) {

        ClientConnect clientConnect = GLOBAL_HOST_INFO_CHANNEL.get(key);
        if (Objects.nonNull(clientConnect)) {
            Channel channel = clientConnect.getChannel();
            if (channel.isActive()) {
                return clientConnect;
            } else {
                GLOBAL_HOST_INFO_CHANNEL.remove(key);
            }
        }
        return null;
    }

    private ClientConnect doCacheConnect(HostInfo hostInfo) {
        // 共享连接（netty的write方法做了线程安全处理，对于外部线程提交的数据，会包装成task加入到队列中，使用netty线程写出）
        String key = hostInfo.getHost() + ":" + hostInfo.getPort();
        ClientConnect clientConnect = this.hitCache(key);
        if (Objects.nonNull(clientConnect)) {
            return clientConnect;
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
        GLOBAL_HOST_INFO_CHANNEL.put(key, client);
        AttributeKey<ClientConnect> attributeKey = AttributeKey.newInstance(channel.id().asLongText());
        channel.attr(attributeKey).set(client);
        return client;
    }

    public List<ClientConnect> getConnects() {
        return KEY_CHANNELS.stream()
            .filter(connect -> Objects.nonNull(connect.getChannel()) && connect.getChannel().isActive())
            .collect(Collectors.toList());
    }

    private void subscribe(ServerInfo serverInfo) {
        this.registryService.subscribe(this, serverInfo);
    }

    private List<HostInfo> initPullService() {
        List<HostInfo> infos = registryService.pullServers(serverInfo);
        this.cacheConnect(infos);
        return infos;
    }
}
