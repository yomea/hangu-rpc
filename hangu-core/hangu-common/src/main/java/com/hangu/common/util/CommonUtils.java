package com.hangu.common.util;

import cn.hutool.core.util.IdUtil;
import com.hangu.common.entity.Response;
import com.hangu.common.entity.RpcResponseTransport;
import com.hangu.common.entity.ServerInfo;
import java.lang.reflect.Constructor;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:16
 */
public final class CommonUtils {

    private CommonUtils() {
        throw new RuntimeException("不允许实例化！");
    }

    public static Long snowFlakeNextId() {

        return IdUtil.createSnowflake(1, 1).nextId();
    }

    public static String createServiceKey(String groupName, String interfaceName, String version) {

        return groupName + "/" + version + "/" + interfaceName;
    }

    public static String createServiceKey(ServerInfo serverInfo) {

        return serverInfo.getGroupName() + "/" + serverInfo.getVersion() + "/" + serverInfo.getInterfaceName();
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


    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... paramTypes) {
        try {
            return clazz.getConstructor(paramTypes);
        }
        catch (NoSuchMethodException ex) {
            return null;
        }
    }

}
