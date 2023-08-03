package com.hanggu.consumer.manager;

import com.hanggu.common.entity.RpcRequestPromise;
import com.hanggu.common.entity.RpcResult;
import io.netty.util.concurrent.DefaultPromise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:57
 */
public class RpcRequestManager {

    private static final Map<Long, RpcRequestPromise<RpcResult>> FUTURE_MAP = new ConcurrentHashMap<>(8192);

    public static void putFuture(Long id, RpcRequestPromise<RpcResult> future) {
        FUTURE_MAP.put(id, future);
    }

    public static RpcRequestPromise<RpcResult> getFuture(Long id) {
        return FUTURE_MAP.get(id);
    }
}
