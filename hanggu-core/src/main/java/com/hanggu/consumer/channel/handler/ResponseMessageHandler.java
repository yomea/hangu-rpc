package com.hanggu.consumer.channel.handler;

import com.hanggu.common.entity.Response;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 处理提供者的返回响应
 * @author wuzhenhong
 * @date 2023/8/2 10:36
 */
public class ResponseMessageHandler extends SimpleChannelInboundHandler<Response> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

    }
}
