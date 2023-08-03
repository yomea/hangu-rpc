package com.hanggu.common.util;

import com.hanggu.common.entity.Response;
import com.hanggu.common.entity.RpcResponseTransport;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:16
 */
public final class CommonUtils {

    private CommonUtils() {
        throw new RuntimeException("不允许实例化！");
    }

    private static final AtomicLong ATOMIC_LONG = new AtomicLong(1);

    public static final Long incrementId() {
        return ATOMIC_LONG.incrementAndGet();
    }

    public static String createServiceKey(String groupName, String interfaceName, String version) {

        return groupName + "/" + version + "/" + interfaceName;
    }

    public static Response createResponseInfo(
        Long id,
        byte serializationType,
        int code,
        Class<?> clzz,
        Object value) {

        RpcResponseTransport rpcResponseTransport = new RpcResponseTransport();
        rpcResponseTransport.setCode(code);
        rpcResponseTransport.setType(clzz);
        rpcResponseTransport.setVale(value);
        // 返回响应
        Response response = new Response();
        response.setId(id);
        response.setSerializationType(serializationType);
        response.setRpcResponseTransport(rpcResponseTransport);

        return response;
    }

    public static ClassLoader getClassLoader(Class<?> cls) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = cls.getClassLoader();
        }
        return cl;
    }

}
