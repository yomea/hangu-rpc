package com.hanggu.provider.server;

import com.hanggu.common.channel.handler.CommonMessageDecoder;
import com.hanggu.common.channel.handler.HeartBeatMsgHandler;
import com.hanggu.common.channel.handler.ResponseMessageEncoder;
import com.hanggu.common.constant.HangguCons;
import com.hanggu.provider.channel.handler.RequestMessageHandler;
import com.hanggu.provider.properties.ProviderProperties;
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
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:23
 */
@Slf4j
public class NettyServerBoostrap {

    private ServerBootstrap serverBootstrap;

    private Channel channel;

    private NioEventLoopGroup boss;

    private NioEventLoopGroup worker;

    public void start(ProviderProperties properties, Executor executor) {

        boss = new NioEventLoopGroup();
        worker = new NioEventLoopGroup(HangguCons.DEF_IO_THREADS);
        try {
            serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class)
                    .group(boss, worker)
                    .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                    .childOption(ChannelOption.SO_REUSEADDR, Boolean.TRUE)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(2048, 11, 4, 0, 0));
                            ch.pipeline().addLast(new ResponseMessageEncoder());
                            ch.pipeline().addLast(new CommonMessageDecoder());
                            ch.pipeline().addLast(new IdleStateHandler(0, 0, 4, TimeUnit.SECONDS));
                            ch.pipeline().addLast(new HeartBeatMsgHandler());
                            ch.pipeline().addLast(new RequestMessageHandler(executor));
                        }
                    });
            channel = serverBootstrap.bind(properties.getPort()).sync().channel();
        } catch (Exception e) {
            log.error("启动服务失败！", e);
            this.close();
        }
    }

    public void close() {
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }

        try {
            if (serverBootstrap != null) {
                boss.shutdownGracefully();
                worker.shutdownGracefully();
            }
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }

    public boolean isActive() {
        return channel.isActive();
    }
}
