package com.hangu.common.manager;

import com.hangu.common.entity.HostInfo;
import com.hangu.common.properties.HanguProperties;
import com.hangu.consumer.client.NettyClient;
import com.hangu.consumer.manager.NettyClientEventLoopManager;
import com.hangu.provider.manager.NettyServerSingleManager;
import com.hangu.provider.server.NettyServer;
import java.util.concurrent.Executor;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:04
 */
public class HanguRpcManager {

    public static final NettyServer openServer(HanguProperties properties) {
        Executor executor = HanguExecutorManager.openIoExecutor(properties.getCoreNum(), properties.getMaxNum());
        NettyServer server = NettyServerSingleManager.openServer(executor, properties.getProvider());
        return server;
    }

    public static final NettyClient openClient(HanguProperties properties, HostInfo hostInfo) {
        Executor executor = HanguExecutorManager.openIoExecutor(properties.getCoreNum(), properties.getMaxNum());
        HanguExecutorManager.openScheduledExecutor();
        NettyClient nettyClient = new NettyClient(hostInfo);
        nettyClient.open(executor);
        return nettyClient;
    }

    public static final NettyServer getNettyServer() {
        return NettyServerSingleManager.getNettyServer();
    }

    public static final void closeClient() {
        NettyClientEventLoopManager.close();
        HanguExecutorManager.close();
    }

    public static final void closeServer() {
        NettyServerSingleManager.closeServer();
        HanguExecutorManager.close();
    }
}
