package com.hanggu.provider.channel.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:56
 */
public class HeartBeatMsgHandler extends SimpleChannelInboundHandler<> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {

    }
}
