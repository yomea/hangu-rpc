package com.hanggu.common.registry.impl;

import cn.hutool.core.map.MapUtil;
import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.RegistryInfo;
import com.hanggu.common.entity.ServerInfo;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.registry.RegistryService;
import com.hanggu.provider.RegistryConstants;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
        String key = this.createKey(registryInfo);
        String value = registryInfo.getHostInfo().toString();
        // 一分钟过期
        String expire = String.valueOf(System.currentTimeMillis() + 60L);
        try {
            jedis.hset(key, value, expire);
            jedis.publish(key, RegistryConstants.REGISTER);
        } catch (Exception e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                String.format("groupName：%s，interfaceName：%s, version：%s 注册到redis失败！", registryInfo.getGroupName(),
                    registryInfo.getInterfaceName(), registryInfo.getVersion()));
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

    @Override
    public List<HostInfo> pullServers(ServerInfo serverInfo) {
        Jedis jedis = this.jedisSentinelPool.getResource();
        String key = this.createKey(serverInfo);
        Map<String, String> valueMapExpire = jedis.hgetAll(key);
        if (MapUtil.isEmpty(valueMapExpire)) {
            return Collections.emptyList();
        }
        long currentTime = System.currentTimeMillis();
        return valueMapExpire.entrySet().stream().filter(entry -> {
            Long expire = Long.parseLong(entry.getValue());
            return currentTime <= expire;
        }).map(entry -> {
            String hostIpStr = entry.getKey();
            String[] hostArr = hostIpStr.split(":");
            HostInfo hostInfo = new HostInfo();
            hostInfo.setHost(hostArr[0]);
            hostInfo.setPort(Integer.parseInt(hostArr[1]));
            return hostInfo;
        }).collect(Collectors.toList());
    }

    private String createKey(ServerInfo serverInfo) {

        String groupName = serverInfo.getGroupName();
        String interfaceName = serverInfo.getInterfaceName();
        String version = serverInfo.getVersion();
        String key = groupName + "/" + interfaceName + "/" + version;

        return key;
    }
}
