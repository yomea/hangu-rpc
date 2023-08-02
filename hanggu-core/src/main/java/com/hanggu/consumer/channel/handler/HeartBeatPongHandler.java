package com.hanggu.consumer.channel.handler;

import com.hanggu.common.entity.PingPong;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.client.NettyClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.net.SocketAddress;

/**
 * 心跳处理器
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:40
 */
public class HeartBeatPongHandler extends SimpleChannelInboundHandler<PingPong> {

    private NettyClient nettyClient;
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
            if(IdleState.WRITER_IDLE == idleState) {
                // 重连
                PingPong pingPong = new PingPong();
                pingPong.setId(CommonUtils.incrementId());
                pingPong.setSerializationType(SerializationTypeEnum.HESSIAN.getType());
                ctx.writeAndFlush(pingPong);
            } else if(IdleState.READER_IDLE == idleState) {
                // 如果该通道超过两倍心跳都没有接收到任何读事件，包括response和心跳响应，那么尝试重连
                SocketAddress remoteAddress = ctx.channel().remoteAddress();
                // 重连创建一个新的通道
                nettyClient.reconnect(remoteAddress);
                // 关闭当前通道
                ctx.channel().close();
            }
        }
    }
}
