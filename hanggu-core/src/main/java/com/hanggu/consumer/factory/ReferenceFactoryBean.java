package com.hanggu.consumer.factory;

import com.hanggu.common.entity.MethodInfo;
import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.ServerInfo;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.enums.MethodCallTypeEnum;
import com.hanggu.common.exception.RpcParseException;
import com.hanggu.common.registry.RegistryService;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.consumer.annotation.HanguMethod;
import com.hanggu.consumer.callback.RpcResponseCallback;
import com.hanggu.consumer.invocation.RpcReferenceHandler;
import com.hanggu.consumer.manager.ConnectManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:13
 */
public class ReferenceFactoryBean<T> implements FactoryBean<T>, InitializingBean {

    private ServerInfo serverInfo;

    private Class<T> interfaceClass;

    private Map<Method, MethodInfo> methodInfoCache;

    private ConnectManager connectManager;

    @Autowired
    private RegistryService registryService;

    public ReferenceFactoryBean(String groupName, String interfaceName, String version, Class<T> interfaceClass) {
        if (!StringUtils.hasText(interfaceName)) {
            interfaceName = interfaceClass.getName();
        }
        this.serverInfo = new ServerInfo(groupName, interfaceName, version);
        this.interfaceClass = interfaceClass;
    }

    @Override
    public T getObject() throws Exception {
        ClassLoader classLoader = CommonUtils.getClassLoader(ReferenceFactoryBean.class);
        RpcReferenceHandler rpcReferenceHandler =
            new RpcReferenceHandler(this.serverInfo, this.connectManager, this.methodInfoCache);

        return (T) Proxy.newProxyInstance(classLoader, new Class<?>[]{interfaceClass}, rpcReferenceHandler);
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.buildMethodInfoCache();
        // 初始化本地服务列表
        this.initLocalServiceDirectory();
    }

    private void initLocalServiceDirectory() {

        this.connectManager = new ConnectManager(registryService, this.serverInfo);
    }

    private void buildMethodInfoCache() {

        methodInfoCache = MethodIntrospector.selectMethods(interfaceClass,
            (MetadataLookup<MethodInfo>) method -> {

                MethodInfo info = new MethodInfo();
                info.setName(method.getName());
                info.setCallType(MethodCallTypeEnum.SYNC.getType());
                info.setTimeout(5);
                info.setCallback(null);

                Class<?>[] parameterTypes = method.getParameterTypes();
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
                HanguMethod hanguMethod = AnnotationUtils.getAnnotation(method, HanguMethod.class);
                if (Objects.nonNull(hanguMethod)) {
                    int timeout = hanguMethod.timeout();
                    if (timeout <= 0) {
                        throw new RpcParseException(ErrorCodeEnum.FAILURE.getCode(),
                            this.msgPrefix(method) + "超时时间必须是大于零数字！");
                    }

                    Class<RpcResponseCallback> callbackClass = hanguMethod.callback();
                    if (!ClassUtils.hasConstructor(callbackClass)) {
                        throw new RpcParseException(ErrorCodeEnum.FAILURE.getCode(),
                            this.msgPrefix(method) + "回调函数没有默认构造器！");
                    }
                    try {
                        RpcResponseCallback callback = ClassUtils.getConstructorIfAvailable(callbackClass)
                            .newInstance();
                        info.setCallType(MethodCallTypeEnum.ASYNC_SPECIFY.getType());
                        info.setTimeout(hanguMethod.timeout());
                        info.setCallback(callback);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                return info;
            });
    }

    private String msgPrefix(Method method) {
        return String.format(
            "group: %s, interfaceName: %s, version: %s, interfaceClass: %s，methodName：%s, "
                + "parameterTypes: %s",
            this.serverInfo.getGroupName(), this.serverInfo.getInterfaceName(), this.serverInfo.getVersion(),
            this.interfaceClass.getName(),
            method.getName(), Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(
                Collectors.joining(",")));
    }
}
