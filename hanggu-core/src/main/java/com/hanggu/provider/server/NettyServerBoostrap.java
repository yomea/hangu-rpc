package com.hanggu.provider.server;

import com.hanggu.common.channel.handler.ByteFrameDecoder;
import com.hanggu.common.constant.HangguCons;
import com.hanggu.provider.channel.handler.HeartBeatPingHandler;
import com.hanggu.provider.channel.handler.RequestMessageHandler;
import com.hanggu.provider.channel.handler.ResponseMessageCodec;
import com.hanggu.provider.properties.ProviderProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
                        ch.pipeline().addLast("logging", new LoggingHandler(LogLevel.INFO))
                            // 读写时间超过4s，表示该链接已失效
                            .addLast(new IdleStateHandler(0, 0, 8, TimeUnit.SECONDS))
                            // 继承 LengthFieldBasedFrameDecoder 用于拆包
                            .addLast(new ByteFrameDecoder())
                            // 用于编解码
                            .addLast(new ResponseMessageCodec())
                            // 心跳处理器
                            .addLast(new HeartBeatPingHandler())
                            // 请求事件处理器，用于调用业务逻辑
                            .addLast(new RequestMessageHandler(executor));
                    }
                });
            channel = serverBootstrap.bind(properties.getPort()).channel();
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
