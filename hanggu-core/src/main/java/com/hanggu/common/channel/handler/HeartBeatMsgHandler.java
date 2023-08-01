package com.hanggu.common.channel.handler;

import com.hanggu.common.entity.PingPong;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:56
 */
@Slf4j
public class HeartBeatMsgHandler extends SimpleChannelInboundHandler<PingPong> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, PingPong msg) throws Exception {
        // 回复心跳，表示链接是通的
        ctx.writeAndFlush(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            IdleState idleState = stateEvent.state();
            if(IdleState.ALL_IDLE == idleState) {
                // 关闭连接
                ctx.channel().close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("心跳处理失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}
