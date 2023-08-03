package com.hanggu.common.manager;

import com.hanggu.consumer.client.NettyClient;
import com.hanggu.provider.properties.ProviderProperties;
import com.hanggu.provider.server.NettyServer;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:04
 */
public class HanguRpcManager {

    private static final Object CLIENT_LOCK = new Object();
    private static final Object SERVER_LOCK = new Object();
    private static volatile NettyClient NETTY_CLIENT;
    private static volatile NettyServer NETTY_SERVER;

    public static final NettyServer openServer(ProviderProperties properties, Executor executor) {
        if (Objects.nonNull(NETTY_SERVER)) {
            return NETTY_SERVER;
        }
        synchronized (SERVER_LOCK) {
            if (Objects.nonNull(NETTY_SERVER)) {
                return NETTY_SERVER;
            }
            NETTY_SERVER = new NettyServer();
            NETTY_SERVER.start(properties, executor);
        }

        return NETTY_SERVER;
    }

    public static final NettyClient openClient() {
        if (Objects.nonNull(NETTY_CLIENT)) {
            return NETTY_CLIENT;
        }
        synchronized (CLIENT_LOCK) {
            if (Objects.nonNull(NETTY_CLIENT)) {
                return NETTY_CLIENT;
            }
            NETTY_CLIENT = new NettyClient();
            NETTY_CLIENT.start();
        }
        return NETTY_CLIENT;
    }

    public static final NettyServer getNettyServer() {
        return NETTY_SERVER;
    }

    public static final NettyClient getNettyClient() {
        return NETTY_CLIENT;
    }
}
