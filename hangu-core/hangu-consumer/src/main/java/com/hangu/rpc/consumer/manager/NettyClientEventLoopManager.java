package com.hangu.rpc.consumer.manager;

import com.hangu.rpc.common.constant.hanguCons;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author wuzhenhong
 * @date 2024/3/7 10:00
 */
public class NettyClientEventLoopManager {

    private static final NioEventLoopGroup NIO_EVENT_LOOP_GROUP = new NioEventLoopGroup(hanguCons.DEF_IO_THREADS << 3);

    public static final NioEventLoopGroup getEventLoop() {
        return NIO_EVENT_LOOP_GROUP;
    }

    public static final void close() {
        NIO_EVENT_LOOP_GROUP.shutdownGracefully();
    }

}
