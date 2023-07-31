package com.hanggu.provider.server;

import com.hanggu.provider.channel.handler.ProtocolMessageCoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:23
 */
@Slf4j
public class NettyServerBoostrap {

    private static final int NCPUS = Runtime.getRuntime().availableProcessors();

    public static void start() {
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup(NCPUS);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.channel(NioServerSocketChannel.class);
            serverBootstrap.group(boss, worker);
            serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024, 6, 4, 0, 0));
                    ch.pipeline().addLast(new ProtocolMessageCoder());
                    ch.pipeline().addLast(new IdleStateHandler(0, 0, 4, TimeUnit.SECONDS));
                    ch.pipeline().addLast(new ServerPingPongMessageHandler());
                    ch.pipeline().addLast(new RpcServerMessageHandler(threadPoolProvider));
                }
            });
            Channel channel = serverBootstrap.bind(DaoCloudServerProperties.serverPort).sync().channel();
            log.info(">>>>>>>>>>> start server port = {} bingo <<<<<<<<<<", DaoCloudServerProperties.serverPort);
            channel.closeFuture().sync();
        } catch (Exception e) {
            log.error("<<<<<<<<<<< start dao server interrupted error >>>>>>>>>>>");
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
