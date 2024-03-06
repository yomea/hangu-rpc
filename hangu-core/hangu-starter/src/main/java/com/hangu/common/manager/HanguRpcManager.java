package com.hangu.common.manager;

import cn.hutool.core.net.NetUtil;
import com.hangu.common.constant.hanguCons;
import com.hangu.common.entity.HostInfo;
import com.hangu.common.properties.HanguProperties;
import com.hangu.consumer.client.NettyClient;
import com.hangu.consumer.manager.NettyClientSingleManager;
import com.hangu.provider.manager.NettyServerSingleManager;
import com.hangu.provider.server.NettyServer;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:04
 */
public class HanguRpcManager {

    private static final Object EXECUTOR_LOCK = new Object();
    private static final Object SCHEDULE_LOCK = new Object();

    private static volatile ExecutorService GLOBAL_EXECUTOR;
    private static volatile ScheduledExecutorService SCHEDULE_EXECUTOR;

    private static HostInfo LOCAL_HOST;

    public static final NettyServer openServer(HanguProperties properties) {
        Executor executor = openIoExecutor(properties);
        NettyServer server = NettyServerSingleManager.openServer(executor, properties.getProvider());
        HostInfo hostInfo = new HostInfo();
        hostInfo.setHost(NetUtil.getLocalhost().getHostAddress());
        hostInfo.setPort(properties.getProvider().getPort());
        LOCAL_HOST = hostInfo;
        return server;
    }

    public static final NettyClient openClient(HanguProperties properties) {
        Executor executor = openIoExecutor(properties);
        openScheduledExecutor();
        return NettyClientSingleManager.openClient(executor);
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
            coreNum = coreNum <= 0 ? hanguCons.DEF_IO_THREADS * 4 : coreNum;
            int maxNum = hanguProperties.getMaxNum();
            maxNum = maxNum <= 0 ? hanguCons.CPUS * 8 : maxNum;

            ExecutorService executor = new ThreadPoolExecutor(coreNum, maxNum,
                10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10000));

            GLOBAL_EXECUTOR = executor;
        }
        return GLOBAL_EXECUTOR;
    }

    public static final Executor openScheduledExecutor() {
        if (Objects.nonNull(SCHEDULE_EXECUTOR)) {
            return SCHEDULE_EXECUTOR;
        }
        synchronized (SCHEDULE_LOCK) {
            if (Objects.nonNull(SCHEDULE_EXECUTOR)) {
                return SCHEDULE_EXECUTOR;
            }
            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(hanguCons.CPUS);

            SCHEDULE_EXECUTOR = executor;
        }
        return SCHEDULE_EXECUTOR;
    }

    public static final NettyServer getNettyServer() {
        return NettyServerSingleManager.getNettyServer();
    }

    public static final NettyClient getNettyClient() {
        return NettyClientSingleManager.getNettyClient();
    }

    public static final Executor getGlobalExecutor() {
        return GLOBAL_EXECUTOR;
    }

    public static final HostInfo getLocalHost() {
        return LOCAL_HOST;
    }

    public static final ScheduledExecutorService getSchedule() {
        return SCHEDULE_EXECUTOR;
    }

    public static final void closeClient() {
        NettyClientSingleManager.closeClient();
        if (Objects.nonNull(GLOBAL_EXECUTOR) && !GLOBAL_EXECUTOR.isShutdown()) {
            GLOBAL_EXECUTOR.shutdown();
        }
        if (Objects.nonNull(SCHEDULE_EXECUTOR) && !SCHEDULE_EXECUTOR.isShutdown()) {
            SCHEDULE_EXECUTOR.shutdown();
        }
    }

    public static final void closeServer() {
        NettyServerSingleManager.closeServer();
        if (Objects.nonNull(GLOBAL_EXECUTOR) && !GLOBAL_EXECUTOR.isShutdown()) {
            GLOBAL_EXECUTOR.shutdown();
        }
    }
}
