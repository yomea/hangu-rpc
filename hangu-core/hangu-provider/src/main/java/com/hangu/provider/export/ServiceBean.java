package com.hangu.provider.export;

import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import com.hangu.provider.invoker.RpcInvoker;
import com.hangu.provider.manager.LocalServiceManager;
import com.hangu.provider.manager.NettyServerSingleManager;
import com.hangu.provider.resolver.MethodArgumentResolverHandler;
import java.util.Objects;

/**
 * @author wuzhenhong
 * @date 2023/8/17 15:12
 */
public class ServiceBean<T> {

    private ServerInfo serverInfo;

    private Class<T> interfaceClass;

    private T service;

    private RegistryService registryService;

    private MethodArgumentResolverHandler methodArgumentResolverHandler;

    public ServiceBean(ServerInfo serverInfo,
        Class<T> interfaceClass, T service, RegistryService registryService) {
        this.serverInfo = serverInfo;
        this.interfaceClass = interfaceClass;
        this.service = service;
        this.registryService = registryService;
        this.methodArgumentResolverHandler = MethodArgumentResolverHandler.DEFAULT_RESOLVER;
    }

    public ServiceBean(ServerInfo serverInfo,
        Class<T> interfaceClass, T service, RegistryService registryService,
        MethodArgumentResolverHandler methodArgumentResolverHandler) {
        this.serverInfo = serverInfo;
        this.interfaceClass = interfaceClass;
        this.service = service;
        this.registryService = registryService;
        this.methodArgumentResolverHandler = Objects.isNull(methodArgumentResolverHandler)
            ? MethodArgumentResolverHandler.DEFAULT_RESOLVER
            : methodArgumentResolverHandler;
    }

    public void init() {

        String key = CommonUtils.createServiceKey(this.serverInfo);
        RpcInvoker rpcInvoker = new RpcInvoker(this.service, this.methodArgumentResolverHandler);
        // export service
        // step 1.0 本地暴露
        LocalServiceManager.register(key, rpcInvoker);

        // step 2.0 远程暴露
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setGroupName(this.serverInfo.getGroupName());
        registryInfo.setInterfaceName(this.serverInfo.getInterfaceName());
        registryInfo.setVersion(this.serverInfo.getVersion());
        registryInfo.setHostInfo(NettyServerSingleManager.getLocalHost());
        this.registryService.register(registryInfo);
    }

}
