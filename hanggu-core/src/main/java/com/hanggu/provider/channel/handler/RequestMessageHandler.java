package com.hanggu.provider.channel.handler;

import com.hanggu.common.entity.*;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.NoServiceFoundException;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.provider.invoker.RpcInvoker;
import com.hanggu.provider.manager.LocalServiceManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 处理请求消息
 *
 * @author wuzhenhong
 * @date 2023/8/1 14:03
 */
public class RequestMessageHandler extends SimpleChannelInboundHandler<Request> {

    private Executor executor;

    public RequestMessageHandler(Executor executor) {
        this.executor = executor;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request msg) throws Exception {

        RpcRequestTransport invokerTransport = msg.getInvokerTransport();
        String groupName = invokerTransport.getGroupName();
        String interfaceName = invokerTransport.getInterfaceName();
        String version = invokerTransport.getVersion();
        String key = CommonUtils.createServiceKey(groupName, interfaceName, version);
        RpcInvoker rpcInvoker = LocalServiceManager.get(key);
        if (Objects.isNull(rpcInvoker)) {
            // 未找到对应的服务
            NoServiceFoundException exception =
                    new NoServiceFoundException(String.format("服务名为%s的接口未注册！", key));
            Response response = CommonUtils.createResponseInfo(msg.getId(), msg.getSerializationType(), ErrorCodeEnum.NOT_FOUND.getCode(), NoServiceFoundException.class, exception);
            ctx.writeAndFlush(response);
        } else {
            String methodName = invokerTransport.getMethodName();
            List<ParameterInfo> parameterInfos =
                    Optional.ofNullable(invokerTransport.getParameterInfos()).orElse(Collections.emptyList());
            List<Class<?>> parameterTypeList = parameterInfos.stream().map(ParameterInfo::getType)
                    .collect(Collectors.toList());
            List<Object> parameterValueList = parameterInfos.stream().map(ParameterInfo::getValue)
                    .collect(Collectors.toList());

            RpcInvokerContext context = new RpcInvokerContext();
            context.setRequest(msg);
            context.setCtx(ctx);
            context.setMethodName(methodName);
            context.setParameterTypeList(parameterTypeList);
            context.setParameterValueList(parameterValueList);

            // 线程池调用
            executor.execute(() -> {
                rpcInvoker.invoke(context);
            });
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
