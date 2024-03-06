package com.hangu.provider.manager;

import com.hangu.provider.properties.ProviderProperties;
import com.hangu.provider.server.NettyServer;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2024/3/6 17:58
 */
public class NettyServerSingleManager {

    private static final Object SERVER_LOCK = new Object();

    private static volatile NettyServer NETTY_SERVER;

    public static final NettyServer openServer(Executor executor, ProviderProperties properties) {

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

    public static final NettyServer getNettyServer() {
        return NETTY_SERVER;
    }

    public static final void closeServer() {
        if(Objects.nonNull(NETTY_SERVER)) {
            NETTY_SERVER.close();
        }
    }
}
