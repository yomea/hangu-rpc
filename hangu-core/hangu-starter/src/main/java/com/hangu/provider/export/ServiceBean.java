package com.hangu.provider.export;

import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.manager.HanguRpcManager;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import com.hangu.provider.invoker.RpcInvoker;
import com.hangu.provider.manager.LocalServiceManager;

/**
 * @author wuzhenhong
 * @date 2023/8/17 15:12
 */
public class ServiceBean<T> {

    private ServerInfo serverInfo;

    private Class<T> interfaceClass;

    private T service;

    private RegistryService registryService;

    public ServiceBean(ServerInfo serverInfo,
        Class<T> interfaceClass, T service, RegistryService registryService) {
        this.serverInfo = serverInfo;
        this.interfaceClass = interfaceClass;
        this.service = service;
        this.registryService = registryService;
    }

    public void init() {

        String key = CommonUtils.createServiceKey(serverInfo);
        RpcInvoker rpcInvoker = new RpcInvoker();
        rpcInvoker.setService(service);
        // export service
        // step 1.0 本地暴露
        LocalServiceManager.register(key, rpcInvoker);

        // step 2.0 远程暴露
        RegistryInfo registryInfo = new RegistryInfo();
        registryInfo.setGroupName(serverInfo.getGroupName());
        registryInfo.setInterfaceName(serverInfo.getInterfaceName());
        registryInfo.setVersion(serverInfo.getVersion());
        registryInfo.setHostInfo(HanguRpcManager.getLocalHost());
        registryService.register(registryInfo);
    }

}
