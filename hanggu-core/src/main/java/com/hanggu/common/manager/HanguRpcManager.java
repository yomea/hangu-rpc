package com.hanggu.common.manager;

import cn.hutool.core.net.NetUtil;
import com.hanggu.common.constant.HangguCons;
import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.properties.HanguProperties;
import com.hanggu.consumer.client.NettyClient;
import com.hanggu.provider.server.NettyServer;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:04
 */
public class HanguRpcManager {

    private static final Object CLIENT_LOCK = new Object();
    private static final Object SERVER_LOCK = new Object();

    private static final Object EXECUTOR_LOCK = new Object();
    private static volatile NettyClient NETTY_CLIENT;
    private static volatile NettyServer NETTY_SERVER;

    private static volatile Executor GLOBAL_EXECUTOR;

    private static HostInfo LOCAL_HOST;

    public static final NettyServer openServer(HanguProperties properties) {
        if (Objects.nonNull(NETTY_SERVER)) {
            return NETTY_SERVER;
        }
        Executor executor = openIoExecutor(properties);
        synchronized (SERVER_LOCK) {
            if (Objects.nonNull(NETTY_SERVER)) {
                return NETTY_SERVER;
            }
            NETTY_SERVER = new NettyServer();
            NETTY_SERVER.start(properties.getProvider(), executor);
            HostInfo hostInfo = new HostInfo();
            hostInfo.setHost(NetUtil.getLocalhost().getHostAddress());
            hostInfo.setPort(properties.getProvider().getPort());
            LOCAL_HOST = hostInfo;
        }

        return NETTY_SERVER;
    }

    public static final NettyClient openClient(HanguProperties properties) {
        if (Objects.nonNull(NETTY_CLIENT)) {
            return NETTY_CLIENT;
        }
        Executor executor = openIoExecutor(properties);
        synchronized (CLIENT_LOCK) {
            if (Objects.nonNull(NETTY_CLIENT)) {
                return NETTY_CLIENT;
            }
            NETTY_CLIENT = new NettyClient();
            NETTY_CLIENT.start(executor);
        }
        return NETTY_CLIENT;
    }

    public static final Executor openIoExecutor(HanguProperties hanguProperties) {
        if (Objects.nonNull(GLOBAL_EXECUTOR)) {
            return GLOBAL_EXECUTOR;
        }
        synchronized (EXECUTOR_LOCK) {
            if (Objects.nonNull(GLOBAL_EXECUTOR)) {
                return GLOBAL_EXECUTOR;
            }
            int coreNum = hanguProperties.getCoreNum();
            coreNum = coreNum <= 0 ? HangguCons.DEF_IO_THREADS * 4 : coreNum;
            int maxNum = hanguProperties.getMaxNum();
            maxNum = maxNum <= 0 ? HangguCons.CPUS * 8 : maxNum;

            Executor executor = new ThreadPoolExecutor(coreNum, maxNum,
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10000));

            GLOBAL_EXECUTOR = executor;
        }
        return GLOBAL_EXECUTOR;
    }

    public static final NettyServer getNettyServer() {
        return NETTY_SERVER;
    }

    public static final NettyClient getNettyClient() {
        return NETTY_CLIENT;
    }

    public static final Executor getGlobalExecutor() {
        return GLOBAL_EXECUTOR;
    }

    public static final HostInfo getLocalHost() {
        return LOCAL_HOST;
    }
}
