package com.hanggu.provider.manager;

import com.hanggu.provider.invoker.RpcInvoker;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务向外暴漏时，本地缓存服务键值对
 *
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
public class LocalServiceManager {

    private static final int DEFAULT_SIZE = 1024;

    private static final Map<String, RpcInvoker> SERVICE_CACHE = new ConcurrentHashMap<>(DEFAULT_SIZE);

    public static void register(String key, RpcInvoker rpcInvoker) {

        SERVICE_CACHE.put(key, rpcInvoker);
//        service.getClass().getMethod()
    }

    public static RpcInvoker get(String key) {

        return SERVICE_CACHE.get(key);
    }

    public static RpcInvoker remove(String key) {

        return SERVICE_CACHE.remove(key);
    }
}
