package com.hangu.rpc.provider.manager;

import cn.hutool.core.net.NetUtil;
import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.provider.properties.ProviderProperties;
import com.hangu.rpc.provider.server.NettyServer;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2024/3/6 17:58
 */
public class NettyServerSingleManager {

    private static final Object SERVER_LOCK = new Object();

    private static volatile NettyServer NETTY_SERVER;
    private static HostInfo LOCAL_HOST;

    public static final HostInfo bindLocalHost(ProviderProperties properties) {
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHost(NetUtil.getLocalhost().getHostAddress());
        hostInfo.setPort(properties.getPort());
        LOCAL_HOST = hostInfo;
        return LOCAL_HOST;
    }

    public static final NettyServer openServer(Executor executor, ProviderProperties properties) {

        if (Objects.nonNull(NETTY_SERVER)) {
            return NETTY_SERVER;
        }
        synchronized (SERVER_LOCK) {
            if (Objects.nonNull(NETTY_SERVER)) {
                return NETTY_SERVER;
            }
            bindLocalHost(properties);
            NETTY_SERVER = new NettyServer();
            NETTY_SERVER.start(properties, executor);
        }

        return NETTY_SERVER;
    }

    public static final NettyServer getNettyServer() {
        return NETTY_SERVER;
    }

    public static final HostInfo getLocalHost() {
        return LOCAL_HOST;
    }

    public static final void closeServer() {
        if (Objects.nonNull(NETTY_SERVER)) {
            NETTY_SERVER.close();
        }
    }
}
