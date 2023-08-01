package com.hanggu.provider.channel.handler;

import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.Request;
import com.hanggu.common.entity.Response;
import com.hanggu.common.entity.RpcRequestTransport;
import com.hanggu.common.entity.RpcResponseTransport;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.NoServiceFoundException;
import com.hanggu.common.util.CommonUtils;
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
        Object service = LocalServiceManager.get(key);
        if (Objects.isNull(service)) {
            // 未找到对应的服务
            NoServiceFoundException exception =
                new NoServiceFoundException(String.format("服务名为%s的接口未注册！", key));
            RpcResponseTransport rpcResponseTransport = new RpcResponseTransport();
            rpcResponseTransport.setCode(ErrorCodeEnum.NOT_FOUND.getCode());
            rpcResponseTransport.setType(NoServiceFoundException.class);
            rpcResponseTransport.setVale(exception);
            // 返回响应
            Response response = new Response();
            response.setId(msg.getId());
            response.setSerializationType(msg.getSerializationType());
            response.setRpcResponseTransport(rpcResponseTransport);
            ctx.writeAndFlush(response);
        } else {
            String methodName = invokerTransport.getMethodName();
            List<ParameterInfo> parameterInfos =
                Optional.ofNullable(invokerTransport.getParameterInfos()).orElse(Collections.emptyList());
            List<Class<?>> parameteTypeList = parameterInfos.stream().map(ParameterInfo::getType)
                .collect(Collectors.toList());
            List<Object> parameteValueList = parameterInfos.stream().map(ParameterInfo::getValue)
                .collect(Collectors.toList());

            // 线程池调用
            executor.execute(() -> {
                try {
                    Method method = service.getClass().getMethod(methodName, parameteTypeList.toArray(new Class<?>[0]));
                    method.setAccessible(true);
                    Object result = method.invoke(service, parameteValueList.toArray(new Object[0]));
                    Class<?> returnType = method.getReturnType();
                    RpcResponseTransport rpcResponseTransport = new RpcResponseTransport();
                    rpcResponseTransport.setCode(ErrorCodeEnum.SUCCESS.getCode());
                    rpcResponseTransport.setType(returnType);
                    rpcResponseTransport.setVale(result);
                    // 返回响应
                    Response response = new Response();
                    response.setId(msg.getId());
                    response.setSerializationType(msg.getSerializationType());
                    response.setRpcResponseTransport(rpcResponseTransport);
                    ctx.writeAndFlush(response);
                } catch (NoSuchMethodException e) {
                    // TODO: 2023/8/1 处理错误！反馈给客户端
                } catch (InvocationTargetException e) {
                    // TODO: 2023/8/1 处理错误！反馈给客户端
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    // TODO: 2023/8/1 处理错误！反馈给客户端
                    throw new RuntimeException(e);
                }
            });


        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

}
