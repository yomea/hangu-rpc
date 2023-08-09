package com.hanggu.consumer.invocation;

import cn.hutool.core.util.RandomUtil;
import com.hanggu.common.entity.MethodInfo;
import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.Request;
import com.hanggu.common.entity.RpcRequestPromise;
import com.hanggu.common.entity.RpcRequestTransport;
import com.hanggu.common.entity.RpcResult;
import com.hanggu.common.entity.ServerInfo;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.enums.MethodCallTypeEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.exception.NoServiceFoundException;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.exception.RpcInvokerTimeoutException;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.consumer.client.ClientConnect;
import com.hanggu.consumer.client.NettyClient;
import com.hanggu.consumer.manager.ConnectManager;
import com.hanggu.consumer.manager.RpcRequestManager;
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
import javax.management.ServiceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:17
 */
@Slf4j
public class RpcReferenceHandler implements InvocationHandler {

    private ServerInfo serverInfo;

    private ConnectManager connectManager;

    private Map<Method, MethodInfo> methodInfoCache;

    public RpcReferenceHandler(ServerInfo serverInfo,
        ConnectManager connectManager,
        Map<Method, MethodInfo> methodInfoCache) {
        this.serverInfo = serverInfo;
        this.connectManager = connectManager;
        this.methodInfoCache = methodInfoCache;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String key = CommonUtils.createServiceKey(this.serverInfo);
        List<ClientConnect> connects = this.connectManager.getConnects(key);
        if (CollectionUtils.isEmpty(connects)) {
            throw new ServiceNotFoundException(
                String.format("未找到 groupName = %s, interfaceName = %s, version = %s的有效服务连接地址", this.serverInfo.getGroupName(),
                    this.serverInfo.getInterfaceName(), this.serverInfo.getVersion()));
        }
        // TODO: 2023/8/2 负载均衡，先随便来个随机
        int index = RandomUtil.getRandom().nextInt(0, connects.size());
        ClientConnect connect = connects.get(index);
        // 获取客户端
        NettyClient nettyClient = HanguRpcManager.getNettyClient();
        if (Objects.isNull(nettyClient)) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "请先启动客户端！");
        }
        // 连接
        Channel channel = connect.getChannel();

        MethodInfo methodInfo = methodInfoCache.get(method);

        Request request = new Request();
        request.setId(CommonUtils.snowFlakeNextId());
        request.setSerializationType(SerializationTypeEnum.HESSIAN.getType());

        RpcRequestTransport invokerTransport = new RpcRequestTransport();
        invokerTransport.setGroupName(this.serverInfo.getGroupName());
        invokerTransport.setInterfaceName(this.serverInfo.getInterfaceName());
        invokerTransport.setVersion(this.serverInfo.getVersion());
        invokerTransport.setMethodName(methodInfo.getName());

        List<ParameterInfo> factParameterInfoList = Optional.ofNullable(methodInfo.getFactParameterInfoList())
            .orElse(Collections.emptyList())
            .stream().map(type -> {
                ParameterInfo parameterInfo = new ParameterInfo();
                parameterInfo.setType(type.getType());
                parameterInfo.setIndex(type.getIndex());
                parameterInfo.setValue(args[type.getIndex()]);
                return parameterInfo;
            }).collect(Collectors.toList());
        for (ParameterInfo parameterInfo : factParameterInfoList) {
            parameterInfo.setValue(args[parameterInfo.getIndex()]);
        }

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
        
        channel.writeAndFlush(request).addListener(wFuture -> {
            // 消息发送成功之后，保存请求
            if (wFuture.isSuccess()) {
                RpcRequestManager.putFuture(request.getId(), future);
            } else {
                log.error("发送请求失败！");
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "发送请求异常！");
            }
        });

        if (!MethodCallTypeEnum.SYNC.getType().equals(callType)) {
            HanguRpcManager.getSchedule().schedule(() -> {
                if(future.isSuccess()) {
                    return;
                }
                // 取消
                future.cancel(false);
                List<RpcResponseCallback> callbackList = Optional.ofNullable(future.getCallbacks())
                    .orElse(Collections.emptyList());
                RpcResult rpcResult = new RpcResult();
                rpcResult.setCode(ErrorCodeEnum.TIME_OUT.getCode());
                rpcResult.setReturnType(RpcInvokerTimeoutException.class);
                rpcResult.setResult(new RpcInvokerTimeoutException(ErrorCodeEnum.TIME_OUT.getCode(), "调用超时！"));
                callbackList.stream().forEach(callback -> {
                    callback.callback(rpcResult);
                });
            }, methodInfo.getTimeout(), TimeUnit.SECONDS);
            return null;
        }

        if (!future.await(methodInfo.getTimeout(), TimeUnit.SECONDS)) {
            log.error("请求超时！");
            throw new RpcInvokerException(ErrorCodeEnum.TIME_OUT.getCode(), "请求超时！");
        }

        RpcResult rpcResult = future.getNow();
        return dealRpcResult(rpcResult);
    }

    private Object dealRpcResult(RpcResult rpcResult) throws Throwable {

        if (!ErrorCodeEnum.SUCCESS.getCode().equals(rpcResult.getCode())) {
            Class<?> returnType = rpcResult.getReturnType();
            if (Throwable.class.isAssignableFrom(returnType)) {
                Throwable e = (Throwable) rpcResult.getResult();
                if(e instanceof RpcInvokerException) {
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
