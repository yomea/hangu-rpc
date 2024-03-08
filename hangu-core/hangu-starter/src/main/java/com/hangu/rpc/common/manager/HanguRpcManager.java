package com.hangu.rpc.common.manager;

import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.properties.HanguProperties;
import com.hangu.rpc.consumer.client.NettyClient;
import com.hangu.rpc.consumer.manager.NettyClientEventLoopManager;
import com.hangu.rpc.provider.manager.NettyServerSingleManager;
import com.hangu.rpc.provider.server.NettyServer;
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
