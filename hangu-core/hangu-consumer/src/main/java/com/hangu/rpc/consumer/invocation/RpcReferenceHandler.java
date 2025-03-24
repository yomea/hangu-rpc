package com.hangu.rpc.consumer.invocation;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.hangu.rpc.common.callback.RpcResponseCallback;
import com.hangu.rpc.common.context.HanguContext;
import com.hangu.rpc.common.entity.MethodInfo;
import com.hangu.rpc.common.entity.ParameterInfo;
import com.hangu.rpc.common.entity.Request;
import com.hangu.rpc.common.entity.RequestHandlerInfo;
import com.hangu.rpc.common.entity.RpcRequestPromise;
import com.hangu.rpc.common.entity.RpcRequestTransport;
import com.hangu.rpc.common.entity.RpcResult;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.enums.ErrorCodeEnum;
import com.hangu.rpc.common.enums.MethodCallTypeEnum;
import com.hangu.rpc.common.enums.SerializationTypeEnum;
import com.hangu.rpc.common.exception.NoServiceFoundException;
import com.hangu.rpc.common.exception.RpcInvokerException;
import com.hangu.rpc.common.exception.RpcInvokerTimeoutException;
import com.hangu.rpc.common.manager.HanguExecutorManager;
import com.hangu.rpc.common.util.CommonUtils;
import com.hangu.rpc.common.util.DescClassUtils;
import com.hangu.rpc.consumer.client.ClientConnect;
import com.hangu.rpc.consumer.manager.ConnectManager;
import com.hangu.rpc.consumer.manager.RpcRequestManager;
import io.netty.channel.Channel;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:17
 */
@Slf4j
public class RpcReferenceHandler implements InvocationHandler {

    private RequestHandlerInfo requestHandlerInfo;

    private ConnectManager connectManager;

    private Map<String, MethodInfo> methodInfoCache;

    public RpcReferenceHandler(RequestHandlerInfo requestHandlerInfo,
        ConnectManager connectManager,
        Map<String, MethodInfo> methodInfoCache) {
        this.requestHandlerInfo = requestHandlerInfo;
        this.connectManager = connectManager;
        this.methodInfoCache = methodInfoCache;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        ServerInfo serverInfo = this.requestHandlerInfo.getServerInfo();
        List<ClientConnect> connects = this.connectManager.getConnects();
        if (CollectionUtil.isEmpty(connects)) {
            throw new NoServiceFoundException(ErrorCodeEnum.NOT_FOUND.getCode(),
                String.format(
                    "未找到 groupName = %s, interfaceName = %s, version = %s的有效服务连接地址，请检查是否启动服务提供者！",
                    serverInfo.getGroupName(),
                    serverInfo.getInterfaceName(), serverInfo.getVersion()));
        }
        // TODO: 2023/8/2 负载均衡，先随便来个随机
        int index = RandomUtil.getRandom().nextInt(0, connects.size());
        ClientConnect connect = connects.get(index);
        // 连接
        Channel channel = connect.getChannel();
        MethodInfo providedMethodInfo = this.requestHandlerInfo.getProvidedMethodInfo();
        MethodInfo methodInfo = Objects.nonNull(providedMethodInfo)
            ? providedMethodInfo
            : methodInfoCache.get(DescClassUtils.methodSigName(method));

        Request request = new Request();
        request.setHttp(methodInfo.isHttp());
        request.setId(CommonUtils.snowFlakeNextId());
        request.setSerializationType(SerializationTypeEnum.HESSIAN.getType());

        RpcRequestTransport invokerTransport = new RpcRequestTransport();
        invokerTransport.setGroupName(serverInfo.getGroupName());
        invokerTransport.setInterfaceName(serverInfo.getInterfaceName());
        invokerTransport.setVersion(serverInfo.getVersion());
        invokerTransport.setMethodName(methodInfo.getName());

        List<ParameterInfo> factParameterInfoList = Optional.ofNullable(methodInfo.getFactParameterInfoList())
            .orElse(Collections.emptyList())
            .stream().map(type -> {
                type.setValue(args[type.getIndex()]);
                return type;
            }).collect(Collectors.toList());

        invokerTransport.setParameterInfos(factParameterInfoList);
        request.setInvokerTransport(invokerTransport);
        Integer callType = methodInfo.getCallType();
        List<RpcResponseCallback> callbacks = Collections.emptyList();
        if (MethodCallTypeEnum.ASYNC_PARAMETER.getType().equals(callType)) {
            callbacks = Optional.ofNullable(methodInfo.getCallbackParameterInfoList()).orElse(Collections.emptyList())
                .stream().map(parameterInfo -> (RpcResponseCallback) args[parameterInfo.getIndex()])
                .collect(Collectors.toList());
        } else if (MethodCallTypeEnum.ASYNC_SPECIFY.getType().equals(callType)) {
            callbacks = Collections.singletonList(methodInfo.getCallback());
        }
        RpcRequestPromise<RpcResult> future = new RpcRequestPromise<>(callbacks, channel.eventLoop());
        RpcRequestManager.putFuture(request.getId(), future);
        channel.writeAndFlush(request).addListener(wFuture -> {
            // 消息发送成功之后，保存请求
            if (!wFuture.isSuccess()) {
                RpcInvokerException rpcInvokerException = new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "发送请求异常！");
                future.tryFailure(rpcInvokerException);
                log.error("发送请求失败！");
                throw rpcInvokerException;
            }
        });

        int timeout = this.getTimeout(methodInfo);

        if (!MethodCallTypeEnum.SYNC.getType().equals(callType)) {
            HanguExecutorManager.getSchedule().schedule(() -> {
                if (future.isSuccess()) {
                    return;
                }
                // 取消
                if (!future.cancel(false)) {
                    return;
                }
                List<RpcResponseCallback> callbackList = Optional.ofNullable(future.getCallbacks())
                    .orElse(Collections.emptyList());
                RpcResult rpcResult = new RpcResult();
                rpcResult.setCode(ErrorCodeEnum.TIME_OUT.getCode());
                rpcResult.setReturnType(RpcInvokerTimeoutException.class);
                rpcResult.setResult(new RpcInvokerTimeoutException(ErrorCodeEnum.TIME_OUT.getCode(), "调用超时！"));
                callbackList.stream().forEach(callback -> {
                    callback.callback(rpcResult);
                });
            }, timeout, TimeUnit.SECONDS);
            return null;
        }

        if (future.isDone() && !future.isSuccess()) {
            Throwable cause = future.cause();
            if (Objects.nonNull(cause)) {
                if (cause instanceof RpcInvokerException) {
                    throw cause;
                } else {
                    throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                        "发送请求异常！");
                }
            } else {
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                    "发送请求异常！");
            }
        }

        if (!future.await(timeout, TimeUnit.SECONDS)) {
            log.error("请求超时！");
            throw new RpcInvokerException(ErrorCodeEnum.TIME_OUT.getCode(), "请求超时！");
        }

        RpcResult rpcResult = future.getNow();
        return dealRpcResult(rpcResult);
    }

    private Integer getTimeout(MethodInfo methodInfo) {
        Integer timeout = HanguContext.getValue(HanguContext.DYNAMIC_TIME_OUT);
        if (Objects.nonNull(timeout) && timeout > 0) {
            return timeout;
        }
        timeout = methodInfo.getTimeout();
        if (Objects.nonNull(timeout) && timeout > 0) {
            return timeout;
        }
        return 5;
    }

    private Object dealRpcResult(RpcResult rpcResult) throws Throwable {

        if (!ErrorCodeEnum.SUCCESS.getCode().equals(rpcResult.getCode())) {
            Class<?> returnType = rpcResult.getReturnType();
            if (Throwable.class.isAssignableFrom(returnType)) {
                Throwable e = (Throwable) rpcResult.getResult();
                if (e instanceof RpcInvokerException) {
                    // TODO: 2023/8/7 如果不是业务调用失败产生的错误，那么故障转移或者快速失败
                } else {
                    throw e;
                }
            } else {
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "rpc调用发生了未知错误！");
            }
        }
        return rpcResult.getResult();
    }

}
