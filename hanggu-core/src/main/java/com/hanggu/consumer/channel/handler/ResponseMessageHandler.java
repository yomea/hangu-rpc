package com.hanggu.consumer.channel.handler;

import com.hanggu.common.entity.Response;
import com.hanggu.common.entity.RpcRequestPromise;
import com.hanggu.common.entity.RpcResponseTransport;
import com.hanggu.common.entity.RpcResult;
import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.consumer.manager.RpcRequestManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 处理提供者的返回响应
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:36
 */
@Slf4j
public class ResponseMessageHandler extends SimpleChannelInboundHandler<Response> {

    private Executor executor;

    public ResponseMessageHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

        Long id = response.getId();
        RpcRequestPromise<RpcResult> future = RpcRequestManager.getFuture(id);
        if (Objects.isNull(future) || future.isCancelled()) {
            log.warn("无效的响应请求！id = {}", id);
            return;
        }

        RpcResponseTransport rpcResponseTransport = response.getRpcResponseTransport();
        RpcResult rpcResult = new RpcResult();
        rpcResult.setCode(rpcResponseTransport.getCode());
        rpcResult.setReturnType(rpcResponseTransport.getType());
        rpcResult.setResult(rpcResponseTransport.getVale());

        List<RpcResponseCallback> callbacks = future.getCallbacks();
        if (CollectionUtils.isEmpty(callbacks)) {
            future.setSuccess(rpcResult);
        } else {
            executor.execute(() -> {
                callbacks.stream().forEach(callback -> {
                    callback.callback(rpcResult);
                });
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("处理返回响应失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}
