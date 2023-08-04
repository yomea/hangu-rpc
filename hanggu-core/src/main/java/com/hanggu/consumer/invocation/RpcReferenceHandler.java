package com.hanggu.consumer.invocation;

import cn.hutool.core.util.RandomUtil;
import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.MethodInfo;
import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.Request;
import com.hanggu.common.entity.RpcRequestPromise;
import com.hanggu.common.entity.RpcRequestTransport;
import com.hanggu.common.entity.RpcResult;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.enums.MethodCallTypeEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.consumer.client.NettyClient;
import com.hanggu.consumer.manager.RegistryDirectory;
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

    private String groupName;

    private String interfaceName;

    private String version;

    private RegistryDirectory registryDirectory;

    private Map<Method, MethodInfo> methodInfoCache;

    public RpcReferenceHandler(String groupName, String interfaceName, String version,
        Map<Method, MethodInfo> methodInfoCache) {
        this.groupName = groupName;
        this.interfaceName = interfaceName;
        this.version = version;
        this.methodInfoCache = methodInfoCache;

        this.registryDirectory = new RegistryDirectory();
        // TODO: 2023/8/2 注册订阅 group 的服务拉取监听器
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        String key = CommonUtils.createServiceKey(this.groupName, this.interfaceName, this.version);
        List<HostInfo> hostInfoList = registryDirectory.getByKey(key);
        if (CollectionUtils.isEmpty(hostInfoList)) {
            throw new ServiceNotFoundException(
                String.format("未找到 groupName = %s, interfaceName = %s, version = %s的服务地址", this.groupName,
                    this.interfaceName, this.version));
        }
        // TODO: 2023/8/2 负载均衡，先随便来个随机
        int index = RandomUtil.getRandom().nextInt(0, hostInfoList.size());
        HostInfo hostInfo = hostInfoList.get(index);
        // 获取客户端
        NettyClient nettyClient = HanguRpcManager.getNettyClient();
        if(Objects.isNull(nettyClient)) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "请先启动客户端！");
        }
        // 连接
        Channel channel = nettyClient.connect(hostInfo.getHost(), hostInfo.getPort());

        MethodInfo methodInfo = methodInfoCache.get(method);

        Request request = new Request();
        request.setId(CommonUtils.incrementId());
        // todo：先写死，后边改成配置
        request.setSerializationType(SerializationTypeEnum.HESSIAN.getType());

        RpcRequestTransport invokerTransport = new RpcRequestTransport();
        invokerTransport.setGroupName(this.groupName);
        invokerTransport.setInterfaceName(this.interfaceName);
        invokerTransport.setVersion(this.version);
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
                .stream().map(parameterInfo -> (RpcResponseCallback) args[parameterInfo.getIndex()]).collect(Collectors.toList());
        } else if(MethodCallTypeEnum.ASYNC_SPECIFY.getType().equals(callType)) {
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

        if(!MethodCallTypeEnum.SYNC.getType().equals(callType)) {
            return null;
        }

        if (!future.await(methodInfo.getTimeout(), TimeUnit.SECONDS)) {
            log.error("请求超时！");
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "请求超时！");
        }

        RpcResult rpcResult = future.getNow();
        if (!ErrorCodeEnum.SUCCESS.getCode().equals(rpcResult.getCode())) {
            Class<?> returnType = rpcResult.getReturnType();
            if (Throwable.class.isAssignableFrom(returnType)) {
                Throwable e = (Throwable) rpcResult.getResult();
                throw e;
            } else {
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "rpc调用发生了未知错误！");
            }
        }
        return rpcResult.getResult();
    }

}
