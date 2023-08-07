package com.hanggu.provider.configuration;

import com.hanggu.common.registry.RegistryService;
import com.hanggu.common.registry.impl.RedisRegistryService;
import com.hanggu.provider.listener.ProviderApplicationListener;
import com.hanggu.provider.properties.JedisConfigPropertis;
import java.util.Collections;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
public class ProviderConfiguration {

    @Bean
    public ProviderApplicationListener providerApplicationListener() {
        return new ProviderApplicationListener();
    }

    @ConditionalOnProperty(prefix = "hangu.rpc.registry", name = "protocol", havingValue = "redis")
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JedisConfigPropertis.class)
    public class RedisRegistryConfiguration {

        @Autowired
        private JedisConfigPropertis configPropertis;

        @Bean
        public JedisPoolConfig jedisPoolConfig() {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setTestWhileIdle(true);
            config.setMinEvictableIdleTimeMillis(60000);
            config.setTimeBetweenEvictionRunsMillis(30000);
            config.setNumTestsPerEvictionRun(-1);
            config.setMaxTotal(500);
            config.setMaxIdle(20);
            config.setMinIdle(8);
            return config;
        }

        @Bean
        public JedisSentinelPool jedisSentinelPool(JedisPoolConfig jedisPoolConfig) {
            HashSet<String> infos = new HashSet<>();
            String[] split = configPropertis.getNodes().split(",");
            Collections.addAll(infos, split);
            return new JedisSentinelPool(configPropertis.getMaster(), infos, jedisPoolConfig, 5000,
                configPropertis.getPassword());
        }

        @Bean
        public RegistryService redisRegistryService(JedisSentinelPool jedisSentinelPool) {
            return new RedisRegistryService(jedisSentinelPool);
        }

    }

    @ConditionalOnProperty(prefix = "hangu.rpc.registry", name = "protocol", havingValue = "zookeeper")
    @Configuration(proxyBeanMethods = false)
    public class ZookeeperRegistryConfiguration {


    }

}
