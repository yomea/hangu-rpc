package com.hanggu.consumer.manager;

import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.consumer.client.ClientConnect;
import com.hanggu.consumer.client.NettyClient;
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
 * @date 2023/8/4 15:35
 */
public class ConnectManager {

    private static final Map<String, List<ClientConnect>> KEY_CHANNELS = new ConcurrentHashMap<>(8192);

    public static final void cacheConnects(String key, List<HostInfo> hostInfoList) {
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

    public static final List<ClientConnect> getConnects(String key) {
        List<ClientConnect> connects = KEY_CHANNELS.getOrDefault(key, Collections.emptyList());
        return connects.stream()
            .filter(connect -> Objects.nonNull(connect.getChannel()) && connect.getChannel().isActive())
            .collect(Collectors.toList());
    }

    // TODO: 2023/8/4 定时清理无效的链接（包括下次刷新本地服务列表时清理）
}
