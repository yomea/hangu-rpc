package com.hangu.consumer.channel.handler;

import com.hangu.common.entity.PingPong;
import com.hangu.common.enums.SerializationTypeEnum;
import com.hangu.common.util.CommonUtils;
import com.hangu.consumer.client.ClientConnect;
import com.hangu.consumer.client.NettyClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import java.net.SocketAddress;
import java.util.Objects;
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
        // 收到消息，重置重试发送心跳次数
        this.retryBeat = 0;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            // 读超时，发送心跳
            if (IdleState.READER_IDLE == idleState) {
                if (!ctx.channel().isActive()) {
                    this.reconnect(ctx);
                } else if(retryBeat > 3) {
                    // 重连
                    this.reconnect(ctx);
                } else {
                    PingPong pingPong = new PingPong();
                    pingPong.setId(CommonUtils.snowFlakeNextId());
                    pingPong.setSerializationType(SerializationTypeEnum.HESSIAN.getType());
                    // 发送心跳（从当前 context 往前）
                    ctx.writeAndFlush(pingPong).addListener(future -> {
                        if (!future.isSuccess()) {
                            log.error("发送心跳失败！", future.cause());
                        }
                    });
                    ++retryBeat;
                }
            }
        }
    }

    private void reconnect(ChannelHandlerContext ctx) {
        ClientConnect clientConnect = ctx.channel()
            .attr(AttributeKey.<ClientConnect>valueOf(ctx.channel().id().asLongText())).get();
        ctx.channel().close().addListener(future -> {
            SocketAddress remoteAddress = ctx.channel().remoteAddress();
            if (!future.isSuccess()) {
                log.warn("通道{}关闭失败！", remoteAddress.toString());
                return;
            }
            ctx.channel().eventLoop().execute(() -> {
                // 重连创建一个新的通道
                nettyClient.reconnect(remoteAddress).addListener(f -> {
                    if (!f.isSuccess()) {
                        log.error("重新连接{}失败！", remoteAddress.toString());
                    } else {
                        if (Objects.nonNull(clientConnect)) {
                            ChannelFuture channelFuture = (ChannelFuture) f;
                            clientConnect.updateChannel(channelFuture.channel());
                        }
                    }
                });
            });
        });
    }
}
