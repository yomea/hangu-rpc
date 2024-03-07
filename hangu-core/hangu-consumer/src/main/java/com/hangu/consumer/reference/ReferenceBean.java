package com.hangu.consumer.reference;

import com.hangu.common.callback.RpcResponseCallback;
import com.hangu.common.entity.MethodInfo;
import com.hangu.common.entity.ParameterInfo;
import com.hangu.common.entity.RequestHandlerInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.enums.MethodCallTypeEnum;
import com.hangu.common.exception.RpcParseException;
import com.hangu.common.manager.HanguExecutorManager;
import com.hangu.common.properties.ExecutorProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import com.hangu.consumer.annotation.HanguMethod;
import com.hangu.consumer.invocation.RpcReferenceHandler;
import com.hangu.consumer.manager.ConnectManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/17 15:01
 */
public class ReferenceBean<T> {

    private RequestHandlerInfo requestHandlerInfo;

    private Class<T> interfaceClass;

    private Map<String, MethodInfo> methodInfoCache;

    private ConnectManager connectManager;

    private RegistryService registryService;

    private ExecutorProperties executorProperties;

    public ReferenceBean(RequestHandlerInfo requestHandlerInfo, Class<T> interfaceClass,
        RegistryService registryService, ExecutorProperties executorProperties) {
        ServerInfo serverInfo = requestHandlerInfo.getServerInfo();
        String interfaceName = serverInfo.getInterfaceName();
        if (StringUtils.isBlank(interfaceName)) {
            interfaceName = interfaceClass.getName();
        }
        serverInfo.setInterfaceName(interfaceName);
        this.requestHandlerInfo = requestHandlerInfo;
        this.interfaceClass = interfaceClass;
        this.registryService = registryService;
        this.executorProperties = executorProperties;
    }

    public T buildServiceProxy(ClassLoader classLoader) {
        RpcReferenceHandler rpcReferenceHandler =
            new RpcReferenceHandler(this.requestHandlerInfo, this.connectManager, this.methodInfoCache);

        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{interfaceClass}, rpcReferenceHandler);
    }

    public void init() {
        // 启动全局处理线程
        HanguExecutorManager.openAllGlobalExecutor(executorProperties.getCoreNum(), executorProperties.getMaxNum());

        this.buildMethodInfoCache();
        // 初始化本地服务列表
        this.initLocalServiceDirectory();
    }

    private void initLocalServiceDirectory() {

        this.connectManager = new ConnectManager(registryService, this.requestHandlerInfo.getServerInfo());
    }

    private void buildMethodInfoCache() {

        MethodInfo methodInfo = this.requestHandlerInfo.getProvidedMethodInfo();
        if (Objects.isNull(methodInfo)) {
            Method[] methods = interfaceClass.getMethods();
            this.methodInfoCache = Arrays.stream(methods).map(method -> {

                MethodInfo info = new MethodInfo();
                info.setHttp(false);
                info.setName(method.getName());
                info.setCallType(MethodCallTypeEnum.SYNC.getType());
                info.setTimeout(5);
                info.setCallback(null);

                Class<?>[] parameterTypes = method.getParameterTypes();
                String argsName = Arrays.stream(parameterTypes).map(Class::getName)
                    .collect(Collectors.joining(","));
                info.setSign(method.getName() + "(" + argsName + ")");
                List<ParameterInfo> callbackParameterInfoList = new ArrayList<>();
                List<ParameterInfo> factParameterInfoList = new ArrayList<>();
                for (int index = 0; index < parameterTypes.length; index++) {
                    Class<?> parameterType = parameterTypes[index];
                    ParameterInfo parameterInfo = new ParameterInfo();
                    parameterInfo.setIndex(index);
                    parameterInfo.setType(parameterType);
                    if (RpcResponseCallback.class.isAssignableFrom(parameterType)) {
                        info.setCallType(MethodCallTypeEnum.ASYNC_PARAMETER.getType());
                        callbackParameterInfoList.add(parameterInfo);
                    } else {
                        factParameterInfoList.add(parameterInfo);
                    }
                }
                info.setFactParameterInfoList(factParameterInfoList);
                info.setCallbackParameterInfoList(callbackParameterInfoList);
                // 注解优先级更高
                HanguMethod hanguMethod = method.getAnnotation(HanguMethod.class);
                if (Objects.nonNull(hanguMethod)) {
                    int timeout = hanguMethod.timeout();
                    if (timeout <= 0) {
                        throw new RpcParseException(ErrorCodeEnum.FAILURE.getCode(),
                            this.msgPrefix(method) + "超时时间必须是大于零数字！");
                    }

                    Class<? extends RpcResponseCallback> callbackClass = hanguMethod.callback();
                    if (Objects.isNull(CommonUtils.getConstructor(callbackClass))) {
                        throw new RpcParseException(ErrorCodeEnum.FAILURE.getCode(),
                            this.msgPrefix(method) + "回调函数没有默认构造器！");
                    }
                    try {
                        RpcResponseCallback callback = CommonUtils.getConstructor(callbackClass)
                            .newInstance();
                        info.setCallType(MethodCallTypeEnum.ASYNC_SPECIFY.getType());
                        info.setTimeout(hanguMethod.timeout());
                        info.setCallback(callback);
                        info.setHttp(hanguMethod.http());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                return info;
            }).collect(Collectors.toMap(MethodInfo::getSign, Function.identity()));
        }
    }

    private String msgPrefix(Method method) {
        ServerInfo serverInfo = this.requestHandlerInfo.getServerInfo();
        return String.format(
            "group: %s, interfaceName: %s, version: %s, interfaceClass: %s，methodName：%s, "
                + "parameterTypes: %s",
            serverInfo.getGroupName(), serverInfo.getInterfaceName(), serverInfo.getVersion(),
            this.interfaceClass.getName(),
            method.getName(), Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(
                Collectors.joining(",")));
    }
}
