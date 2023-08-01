package com.hanggu.provider.server;

import com.hanggu.common.channel.handler.HeartBeatMsgHandler;
import com.hanggu.common.channel.handler.ProtocolMessageCoder;
import com.hanggu.provider.channel.handler.RequestMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:23
 */
@Slf4j
public class NettyServerBoostrap {

    private static final int NCPUS = Runtime.getRuntime().availableProcessors();

    private static final ServerBootstrap SERVER_BOOTSTRAP = new ServerBootstrap();

    public static void start(Executor executor) {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(NCPUS);
        try {
            ServerBootstrap serverBootstrap = SERVER_BOOTSTRAP;
            serverBootstrap.channel(NioServerSocketChannel.class)
                .group(boss, worker)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2048, 11, 4, 0, 0));
                        ch.pipeline().addLast(new ProtocolMessageCoder());
                        ch.pipeline().addLast(new IdleStateHandler(0, 0, 4, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new HeartBeatMsgHandler());
                        ch.pipeline().addLast(new RequestMessageHandler(executor));
                    }
                });
            Channel channel = serverBootstrap.bind(8089).sync().channel();
            channel.closeFuture().sync();
        } catch (Exception e) {
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
