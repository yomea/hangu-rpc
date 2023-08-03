package com.hanggu.consumer.channel.handler;

import com.hanggu.common.entity.Response;
import com.hanggu.common.entity.RpcResponseTransport;
import com.hanggu.common.entity.RpcResult;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.consumer.manager.RpcRequestFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.DefaultPromise;
import java.util.Objects;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

/**
 * 处理提供者的返回响应
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:36
 */
@Slf4j
public class ResponseMessageHandler extends SimpleChannelInboundHandler<Response> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

        Long id = response.getId();
        DefaultPromise<RpcResult> future = RpcRequestFuture.getFuture(id);
        if(Objects.isNull(future)) {
            log.warn("无效的响应请求！id = {}", id);
            return;
        }

        RpcResponseTransport rpcResponseTransport = response.getRpcResponseTransport();
        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(rpcResponseTransport.getCode());
        rpcResult.setReturnType(rpcResponseTransport.getType());
        rpcResult.setResult(rpcResponseTransport.getVale());
        future.setSuccess(rpcResult);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理返回响应失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}
