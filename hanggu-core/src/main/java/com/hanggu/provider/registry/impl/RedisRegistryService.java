package com.hanggu.provider.registry.impl;

import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.provider.RegistryConstants;
import com.hanggu.provider.registry.RegistryService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:40
 */
public class RedisRegistryService implements RegistryService {

    private JedisSentinelPool jedisSentinelPool;

    public RedisRegistryService(JedisSentinelPool jedisSentinelPool) {
        this.jedisSentinelPool = jedisSentinelPool;
    }

    @Override
    public void register(RegistryInfo registryInfo) {

        Jedis jedis = this.jedisSentinelPool.getResource();
        String groupName = registryInfo.getGroupName();
        String interfaceName = registryInfo.getInterfaceName();
        String version = registryInfo.getVersion();
        String key = groupName + "/" + interfaceName + "/" + version;
        String value = registryInfo.getHostInfo().toString();
        // 一分钟过期
        String expire = String.valueOf(System.currentTimeMillis() + 60L);
        try {
            jedis.hset(key, value, expire);
            jedis.publish(key, RegistryConstants.REGISTER);
        } catch (Exception e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                String.format("groupName：%s，interfaceName：%s, version：%s 注册到redis失败！", groupName, interfaceName, version));
        } finally {
            // 释放连接
            jedis.close();
        }
    }

    @Override
    public void unRegister(RegistryInfo registryInfo) {

    }

    @Override
    public void subscribe(RegistryInfo registryInfo) {

    }

    @Override
    public void unSubscribe(RegistryInfo registryInfo) {

    }
}
