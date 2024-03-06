package com.hangu.consumer.manager;

import com.hangu.consumer.client.NettyClient;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2024/3/6 17:52
 */
public class NettyClientSingleManager {

    private static final Object CLIENT_LOCK = new Object();

    private static volatile NettyClient NETTY_CLIENT;

    public static final NettyClient openClient(Executor executor) {
        if (Objects.nonNull(NETTY_CLIENT)) {
            return NETTY_CLIENT;
        }
        synchronized (CLIENT_LOCK) {
            if (Objects.nonNull(NETTY_CLIENT)) {
                return NETTY_CLIENT;
            }
            NETTY_CLIENT = new NettyClient();
            NETTY_CLIENT.start(executor);
        }
        return NETTY_CLIENT;
    }

    public static final NettyClient getNettyClient() {
        return NETTY_CLIENT;
    }

    public static final void closeClient() {
        if(Objects.nonNull(NETTY_CLIENT)) {
            NETTY_CLIENT.close();
        }
    }
}
