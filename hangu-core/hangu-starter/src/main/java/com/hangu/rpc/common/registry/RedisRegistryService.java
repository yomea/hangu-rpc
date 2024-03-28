package com.hangu.rpc.common.registry;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import com.hangu.rpc.common.constant.RegistryConstants;
import com.hangu.rpc.common.entity.HostInfo;
import com.hangu.rpc.common.entity.RegistryInfo;
import com.hangu.rpc.common.entity.RegistryNotifyInfo;
import com.hangu.rpc.common.entity.ServerInfo;
import com.hangu.rpc.common.enums.ErrorCodeEnum;
import com.hangu.rpc.common.exception.RpcInvokerException;
import com.hangu.rpc.common.listener.RegistryNotifyListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.JedisSentinelPool;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:40
 */
@Slf4j
public class RedisRegistryService extends AbstractRegistryService {

    private static final Long EXPIRE_TIME = 12L * 1000L;

    private JedisSentinelPool jedisSentinelPool;

    private final ScheduledExecutorService expireExecutor = Executors.newScheduledThreadPool(1,
        new NamedThreadFactory("HanguRedisRegistryExpireTimer", true));

    public RedisRegistryService(JedisSentinelPool jedisSentinelPool) {
        this.jedisSentinelPool = jedisSentinelPool;
        expireExecutor.scheduleWithFixedDelay(() -> this.refreshExpire(), 4, 4, TimeUnit.SECONDS);
    }

    @Override
    public void doRegister(RegistryInfo registryInfo) {
        this.doRegister(Collections.singletonList(registryInfo), RegistryConstants.REGISTER);
    }

    @Override
    public void unRegister(RegistryInfo registryInfo) {
        Jedis jedis = this.jedisSentinelPool.getResource();
        String key = this.createKey(registryInfo);
        String value = registryInfo.getHostInfo().toString();
        try {
            jedis.hdel(key, value);
        } finally {
            jedis.close();
        }

    }

    @Override
    public List<HostInfo> doSubscribe(RegistryNotifyListener listener, ServerInfo serverInfo) {
        String key = this.createKey(serverInfo);

        JedisPubSubNotifier notifier = new JedisPubSubNotifier();
        notifier.setJedisSentinelPool(this.jedisSentinelPool);
        notifier.setKey(key);
        notifier.setRegistryNotifyListener(listener);
        notifier.setServerInfo(serverInfo);
        new SubNotifyThread(notifier).start();
        // 订阅之后，拉取数据，避免在订阅前发生了数据变更，未及时更新本地提供者列表
        return this.pullServers(serverInfo);
    }

    @Override
    protected void doClose() {
        this.jedisSentinelPool.close();
    }

    @Override
    public List<HostInfo> pullServers(ServerInfo serverInfo) {
        Jedis jedis = this.jedisSentinelPool.getResource();
        String key = this.createKey(serverInfo);
        Map<String, String> valueMapExpire;
        try {
            valueMapExpire = jedis.hgetAll(key);
        } finally {
            jedis.close();
        }
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

    private void refreshExpire() {
        try {
            this.doRegister(registered.stream().collect(Collectors.toList()), RegistryConstants.REFRESH);
            this.clearExpireData();
        } catch (Throwable e) {
            log.error("Unexpected exception occur at defer expire time, cause: " + e.getMessage(), e);
        }
    }

    private void clearExpireData() {

        Jedis jedis = jedisSentinelPool.getResource();
        try {
            registered.stream().forEach(serverInfo -> {
                String key = this.createKey(serverInfo);
                Map<String, String> hostMap = jedis.hgetAll(key);
                List<String> expireKeyList = hostMap.entrySet().stream().filter(entry -> {
                    Long time = Long.parseLong(entry.getValue());
                    return System.currentTimeMillis() + EXPIRE_TIME < time;
                }).map(Entry::getKey).collect(Collectors.toList());
                if (CollectionUtil.isNotEmpty(expireKeyList)) {
                    jedis.hdel(key, expireKeyList.toArray(new String[0]));
                }
            });
        } finally {
            jedis.close();
        }
    }

    private void doRegister(List<RegistryInfo> registryInfoList, String publicMessage) {

        if (CollectionUtil.isEmpty(registryInfoList)) {
            return;
        }

        Jedis jedis = this.jedisSentinelPool.getResource();
        try {
            registryInfoList.stream().forEach(registryInfo -> {
                String key = this.createKey(registryInfo);
                String value = registryInfo.getHostInfo().toString();
                // 12秒过期
                String expire = String.valueOf(System.currentTimeMillis() + EXPIRE_TIME);
                try {
                    jedis.hset(key, value, expire);
                    jedis.publish(key, publicMessage);
                } catch (Exception e) {
                    throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                        String.format("groupName：%s，interfaceName：%s, version：%s 注册到redis失败！",
                            registryInfo.getGroupName(),
                            registryInfo.getInterfaceName(), registryInfo.getVersion()));
                }
            });
        } finally {
            // 释放连接
            jedis.close();
        }

    }

    @Data
    private static class JedisPubSubNotifier extends JedisPubSub {

        private RegistryNotifyListener registryNotifyListener;
        private JedisSentinelPool jedisSentinelPool;
        private ServerInfo serverInfo;
        private String key;

        @Override
        public void onMessage(String channel, String message) {
            /*if(RegistryConstants.REFRESH.equals(message)) {

            }*/
            // 刷新
            Jedis jedis = jedisSentinelPool.getResource();
            Map<String, String> hostMap;
            try {
                hostMap = jedis.hgetAll(this.key);
            } finally {
                jedis.close();
            }
            if (Objects.isNull(hostMap) || hostMap.isEmpty()) {
                return;
            }
            long currentTime = System.currentTimeMillis();
            List<HostInfo> hostInfoList = hostMap.entrySet().stream().filter(entry -> {
                Long expire = Long.parseLong(entry.getValue());
                return currentTime <= expire;
            }).map(entry -> {
                String str = entry.getKey();
                String[] arr = str.split(":");
                String host = arr[0];
                Integer port = Integer.parseInt(arr[1]);
                HostInfo hostInfo = new HostInfo();
                hostInfo.setHost(host);
                hostInfo.setPort(port);
                return hostInfo;
            }).collect(Collectors.toList());
            RegistryNotifyInfo notifyInfo = new RegistryNotifyInfo();
            notifyInfo.setServerInfo(this.serverInfo);
            notifyInfo.setHostInfos(hostInfoList);
            registryNotifyListener.registryNotify(notifyInfo);
        }

        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            super.onSubscribe(channel, subscribedChannels);
        }
    }

    private static class SubNotifyThread extends Thread {

        private JedisPubSubNotifier notifier;


        public SubNotifyThread(JedisPubSubNotifier notifier) {
            this.notifier = notifier;
        }

        @Override
        public void run() {

            Jedis jedis = notifier.getJedisSentinelPool().getResource();
            try {
                // 阻塞
                jedis.subscribe(notifier, notifier.getKey());
            } finally {
                jedis.close();
            }
        }
    }
}
