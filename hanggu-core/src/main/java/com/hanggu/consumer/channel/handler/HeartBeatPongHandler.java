package com.hanggu.consumer.channel.handler;

import com.hanggu.common.entity.PingPong;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.client.NettyClient;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳处理器
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:40
 */
@Slf4j
public class HeartBeatPongHandler extends SimpleChannelInboundHandler<PingPong> {

    private NettyClient nettyClient;

    private int retryBeat = 0;

    public HeartBeatPongHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingPong pingPong) throws Exception {
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            // 写超时，发送心跳
            if (IdleState.WRITER_IDLE == idleState) {
                if (!ctx.channel().isActive()) {
                    this.reconnect(ctx);
                } else {
                    PingPong pingPong = new PingPong();
                    pingPong.setId(CommonUtils.incrementId());
                    pingPong.setSerializationType(SerializationTypeEnum.HESSIAN.getType());
                    // 发送心跳（从当前 context 往前）
                    ctx.writeAndFlush(pingPong).addListener(future -> {
                        // 发送失败，有可能是连读断了，也有可能只是网络抖动问题
                        if (!future.isSuccess() && ++retryBeat > 3) {
                            // 重连
                            this.reconnect(ctx);
                        }
                    });
                }
            } else if (IdleState.READER_IDLE == idleState) {
                // 如果该通道超过两倍心跳都没有接收到任何读事件，包括response和心跳响应，那么尝试重连
                this.reconnect(ctx);
            }
        }
    }

    private void reconnect(ChannelHandlerContext ctx) {
        ctx.channel().close().addListener(future -> {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            if (!future.isSuccess()) {
                log.warn("通道{}关闭失败！", remoteAddress.toString());
                return;
            }
            ctx.channel().eventLoop().schedule(() -> {
                // 重连创建一个新的通道
                nettyClient.reconnect(remoteAddress).addListener(f -> {
                    if(!f.isSuccess()) {
                        log.error("重新连接{}失败！", remoteAddress.toString());
                    }
                });
            }, 1, TimeUnit.SECONDS);
        });
    }
}
