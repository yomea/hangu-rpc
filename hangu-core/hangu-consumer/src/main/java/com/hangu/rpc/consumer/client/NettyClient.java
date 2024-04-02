package com.hangu.rpc.consumer.client;

import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.handler.ByteFrameDecoder;
import com.hangu.rpc.common.handler.HeartBeatEncoder;
import com.hangu.rpc.consumer.handler.HeartBeatPongHandler;
import com.hangu.rpc.consumer.handler.RequestMessageCodec;
import com.hangu.rpc.consumer.handler.ResponseMessageHandler;
import com.hangu.rpc.consumer.manager.NettyClientEventLoopManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:19
 */
@Slf4j
public class NettyClient {

    private NioEventLoopGroup NIO_EVENT_LOOP_GROUP = NettyClientEventLoopManager.getEventLoop();

    private Bootstrap bootstrap;

    private HostInfo hostInfo;

    private ClientConnect clientConnect;

    public NettyClient(HostInfo hostInfo) {
        this.hostInfo = hostInfo;
        // 初始化客户端连接
        this.clientConnect = new ClientConnect(null, hostInfo, 20);
    }

    public void open(Executor executor) {
        try {
            this.bootstrap = new Bootstrap();
//            @Sharable
            LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
            bootstrap.group(NIO_EVENT_LOOP_GROUP)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                // 设置水位线
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                            .addLast(new ByteFrameDecoder())
                            .addLast(new RequestMessageCodec()) // 请求与响应编解码器
                            .addLast(new HeartBeatEncoder()) // 心跳编码器
//                            .addLast("logging", loggingHandler)
                            // 每隔 2s 发送一次心跳，超过三次没有收到响应，也就是三倍的心跳时间，重连
                            .addLast(new IdleStateHandler(2, 0, 0, TimeUnit.SECONDS))
                            .addLast(new HeartBeatPongHandler(NettyClient.this)) // 心跳编码器
                            .addLast(new ResponseMessageHandler(executor));
                    }
                });
        } catch (Exception e) {
            log.error("rpc客户端启动失败！", e);
        }
    }

    /**
     * 连接
     *
     * @return
     */
    public ClientConnect syncConnect() throws InterruptedException {
        Channel channel = this.bootstrap.connect(hostInfo.getHost(), hostInfo.getPort()).addListener(future -> {
            if (!future.isSuccess()) {
                log.error("连接 {}:{} 失败！", hostInfo.getHost(), hostInfo.getPort());
            }
        }).sync().channel();
        this.clientConnect.setChannel(channel);
        return this.clientConnect;
    }

    public ChannelFuture reconnect() {
        return this.bootstrap.connect(hostInfo.getHost(), hostInfo.getPort());
    }

    public ClientConnect getClientConnect() {
        return this.clientConnect;
    }

    public HostInfo getHostInfo() {
        return this.hostInfo;
    }
}
