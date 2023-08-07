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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:56
 */
public class ConnectManager implements RegistryNotifyListener {

    private final Map<String, List<ClientConnect>> KEY_CHANNELS = new ConcurrentHashMap<>(8192);

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
        NettyClient nettyClient = HanguRpcManager.getNettyClient();
        List<ClientConnect> clients = hostInfoList.stream().map(hostInfo -> {
            // 注意，这样直接链接的是异步的，获取链接的时候还是需要检查是否链接成功，方可使用
            Channel channel = nettyClient.connect(hostInfo.getHost(), hostInfo.getPort());
            ClientConnect client = new ClientConnect();
            client.setChannel(channel);
            client.setHostInfo(hostInfo);
            return client;
        }).collect(Collectors.toList());

        KEY_CHANNELS.put(key, clients);
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
