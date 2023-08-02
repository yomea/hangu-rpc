package com.hanggu.consumer.invocation;

import cn.hutool.core.util.RandomUtil;
import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.Request;
import com.hanggu.common.entity.RpcRequestTransport;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.manager.HanguRpcManager;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.client.NettyClient;
import com.hanggu.consumer.manager.RegistryDirectory;
import com.hanggu.consumer.manager.RpcRequestFuture;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.DefaultPromise;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public RpcReferenceHandler(String groupName, String interfaceName, String version) {
        this.groupName = groupName;
        this.interfaceName = interfaceName;
        this.version = version;
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
        // 启动客户端
        NettyClient nettyClient = HanguRpcManager.openClient();
        // 连接
        Channel channel = nettyClient.connect(hostInfo.getHost(), hostInfo.getPort());

        Request request = new Request();
        request.setId(CommonUtils.incrementId());
        // todo：先写死，后边改成配置
        request.setSerializationType(SerializationTypeEnum.HESSIAN.getType());

        RpcRequestTransport invokerTransport = new RpcRequestTransport();
        invokerTransport.setGroupName(this.groupName);
        invokerTransport.setInterfaceName(this.interfaceName);
        invokerTransport.setVersion(this.version);
        invokerTransport.setMethodName(method.getName());

        Class<?>[] parameterTypes = method.getParameterTypes();
        List<ParameterInfo> parameterInfos = new ArrayList<>();
        int i = 0;
        for(Class<?> type : parameterTypes) {
            ParameterInfo parameterInfo = new ParameterInfo();
            parameterInfo.setType(type);
            parameterInfo.setValue(args[i++]);
        }

        invokerTransport.setParameterInfos(parameterInfos);

        request.setInvokerTransport(invokerTransport);

        DefaultPromise<Object> future = new DefaultPromise<>(channel.eventLoop());

        channel.writeAndFlush(request).addListener(wFuture -> {
            // 消息发送成功之后，保存请求
            if(wFuture.isSuccess()) {
                RpcRequestFuture.putFuture(request.getId(), future);
            } else {
                log.error("发送请求失败！");
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "发送请求异常！");
            }
        });

        // TODO: 2023/8/2 后边有空加入异步调用，异步调用很简单，就是通过时间轮或者调度线程池，超时还没返回表示失败即可
        if(!future.await(2000, TimeUnit.MILLISECONDS)) {
            log.error("请求超时！");
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "请求超时！");
        }
        
        return future.getNow();
    }
}
