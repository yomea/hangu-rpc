package com.hanggu.provider.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务向外暴漏时，本地缓存服务键值对
 * @author wuzhenhong
 * @date 2023/7/31 15:07
 */
public class LocalServiceManager {

    private static final int DEFAULT_SIZE = 1024;

    private static final Map<String, Object> SERVICE_CACHE = new ConcurrentHashMap<>(DEFAULT_SIZE);

    public static void register(String key, Object service) {

        SERVICE_CACHE.put(key, service);
//        service.getClass().getMethod()
    }
}
