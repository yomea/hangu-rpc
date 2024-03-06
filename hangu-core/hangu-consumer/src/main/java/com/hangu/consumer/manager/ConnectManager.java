package com.hangu.consumer.manager;

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
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:56
 */
@Slf4j
public class ConnectManager implements RegistryNotifyListener {

    private static final Map<String, ClientConnect> GLOBAL_HOST_INFO_CHANNEL = new ConcurrentHashMap<>();
    private static final Object OBJECT = new Object();
    private static final Map<String, Object> GLOBAL_CHANNEL_LOCK = new ConcurrentHashMap<>();

    private List<ClientConnect> KEY_CHANNELS = Collections.emptyList();

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
        this.cacheRegistoryConnect(notifyInfo.getHostInfos());

    }

    /**
     * 每个接口都有一个由自己管理的链接管理器
     * 正常情况下是没有并发的，不过这里做个预防性编程
     *
     * @param hostInfoList
     */
    private synchronized void cacheRegistoryConnect(List<HostInfo> hostInfoList) {
        // 筛选出活着的通道
        List<ClientConnect> activeChannelList = this.KEY_CHANNELS.stream().filter(ClientConnect::isActive)
            .collect(Collectors.toList());
        // 筛选出已断开的链接
        List<ClientConnect> inactiveChannelList = this.KEY_CHANNELS.stream().filter(e -> !e.isActive())
            .collect(Collectors.toList());
        Set<HostInfo> hostInfoSet = hostInfoList.stream().collect(Collectors.toSet());
        // 如果链接已经失效并且注册中心上确实不存在该链接了，那么直接标记为释放，连重试都不需要了
        // 这里可能会因为网络抖动被误认为该地址对应的服务下线，所以需要再下次通知的时候
        // 主动将release修改回去
        inactiveChannelList.stream().forEach(e -> {
            if (!hostInfoSet.contains(e.getHostInfo())) {
                e.markRelease();
            }
        });
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
        List<ClientConnect> newConnectList = hostInfoList.stream().map(hostInfo -> {
            ClientConnect clientConnect = ConnectManager.doCacheConnect(hostInfo);
            // 这里主要是为了解决网络抖动，误判机器下线，等网络正常时，注册中心再次通知
            // 那么需要重新标记为true
            if (clientConnect.isActive()) {
                clientConnect.setRelease(false);
            }
            return clientConnect;
        }).collect(Collectors.toList());
        activeChannelList.addAll(newConnectList);
        this.KEY_CHANNELS = activeChannelList;
    }

    public static ClientConnect hitCache(String key) {

        ClientConnect clientConnect = GLOBAL_HOST_INFO_CHANNEL.get(key);
        if (Objects.nonNull(clientConnect)) {
            // 被标记为release并且失活才会从缓存中移除该链接
            if (clientConnect.isRelease() && !clientConnect.isActive()) {
                removeCache(key);
            } else {
                return clientConnect;
            }
        }
        return null;
    }

    public static void removeCache(HostInfo hostInfo) {
        removeCache(buildHostKey(hostInfo));
    }

    public static void removeCache(String key) {
        GLOBAL_HOST_INFO_CHANNEL.remove(key);
    }

    public static ClientConnect doCacheConnect(HostInfo hostInfo) {
        // 共享连接（netty的write方法做了线程安全处理，对于外部线程提交的数据，会包装成task加入到队列中，使用netty线程写出）
        String key = buildHostKey(hostInfo);
        ClientConnect clientConnect = ConnectManager.hitCache(key);
        if (Objects.nonNull(clientConnect)) {
            return clientConnect;
        }
        while (GLOBAL_CHANNEL_LOCK.putIfAbsent(key, OBJECT) != null) {
            Thread.yield();
        }
        try {
            // 再次检查是否已经缓存了链接，缓存了，直接返回
            clientConnect = ConnectManager.hitCache(key);
            if (Objects.nonNull(clientConnect)) {
                return clientConnect;
            }
            NettyClient nettyClient = NettyClientSingleManager.getNettyClient();
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
        } finally {
            GLOBAL_CHANNEL_LOCK.remove(key);
        }
    }

    private static String buildHostKey(HostInfo hostInfo) {

        return hostInfo.getHost() + ":" + hostInfo.getPort();
    }

    public static void doCacheReconnect(HostInfo hostInfo) {
        String key = buildHostKey(hostInfo);
        // 此处加锁主要为了防止注册中心通知与重连发生线程安全
        // 重连本身是通过监听通道的 unRegister 来触发的，没有线程安全问题
        while (GLOBAL_CHANNEL_LOCK.putIfAbsent(key, OBJECT) != null) {
            Thread.yield();
        }
        try {
            ClientConnect client = hitCache(key);
            // 已被释放的不需要重连
            if (Objects.isNull(client)) {
                return;
            }
            // 已激活不需要重连
            if (client.isActive()) {
                return;
            }
            // 被标记为释放不需要重连
            if (client.isRelease()) {
                return;
            }
            NettyClient nettyClient = NettyClientSingleManager.getNettyClient();
            ChannelFuture channelFuture = nettyClient.reconnect(hostInfo.getHost(), hostInfo.getPort());
            channelFuture.addListener(future -> {
                Channel channel = channelFuture.channel();
                if (!future.isSuccess()) {
                    log.error("链接服务{}失败！", hostInfo);
                } else {
                    client.resetConnCount();
                }
                client.setChannel(channel);
                // 缓存客户端连接，进入下次重试
                AttributeKey<ClientConnect> attributeKey = AttributeKey.newInstance(channel.id().asLongText());
                channel.attr(attributeKey).set(client);
            });
            GLOBAL_HOST_INFO_CHANNEL.put(key, client);
        } finally {
            GLOBAL_CHANNEL_LOCK.remove(key);
        }
    }

    public List<ClientConnect> getConnects() {
        return KEY_CHANNELS.stream()
            .filter(ClientConnect::isActive)
            .collect(Collectors.toList());
    }

    private void subscribe(ServerInfo serverInfo) {
        this.registryService.subscribe(this, serverInfo);
    }

    private List<HostInfo> initPullService() {
        List<HostInfo> infos = registryService.pullServers(serverInfo);
        this.cacheRegistoryConnect(infos);
        return infos;
    }
}
