package com.hanggu.consumer.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:57
 */
public class RpcRequestFuture {

    private static final Map<Long, Future<Object>> FUTURE_MAP = new ConcurrentHashMap<>(8192);

    public static void putFuture(Long id, Future<Object> future) {
        FUTURE_MAP.put(id, future);
    }

    public static Future<Object> getFuture(Long id) {
        return FUTURE_MAP.get(id);
    }
}
