package com.hangu.common.configuration;

import com.hangu.common.manager.HanguRpcManager;
import com.hangu.common.properties.HanguProperties;
import com.hangu.common.properties.JedisConfigPropertis;
import com.hangu.common.properties.ZookeeperConfigProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.registry.RedisRegistryService;
import com.hangu.common.registry.ZookeeperRegistryService;
import com.hangu.consumer.configuration.ConsumerConfiguration;
import com.hangu.provider.configuration.ProviderConfiguration;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

/**
 * Created by wuzhenhong on 2023/8/1 23:53
 */
@Configuration(proxyBeanMethods = false)
@Import({ProviderConfiguration.class, ConsumerConfiguration.class})
public class HanguAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "hangu.rpc")
    public HanguProperties hanguProperties() {

        return new HanguProperties();
    }

    @Bean
    public Executor rpcIoExecutor(HanguProperties hanguProperties) {
        return HanguRpcManager.openIoExecutor(hanguProperties);
    }

    @ConditionalOnProperty(prefix = "hangu.rpc.registry", name = "protocol", havingValue = "redis")
    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JedisConfigPropertis.class)
    public class RedisRegistryConfiguration {

        @Autowired
        private JedisConfigPropertis configPropertis;

        @Bean
        @ConditionalOnMissingBean(JedisPoolConfig.class)
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
        @ConditionalOnMissingBean(JedisSentinelPool.class)
        public JedisSentinelPool jedisSentinelPool(JedisPoolConfig jedisPoolConfig) {
            HashSet<String> infos = new HashSet<>();
            String[] split = configPropertis.getNodes().split(",");
            Collections.addAll(infos, split);
            return new JedisSentinelPool(configPropertis.getMaster(), infos, jedisPoolConfig, 5000,
                configPropertis.getPassword());
        }

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean(RegistryService.class)
        public RegistryService redisRegistryService(JedisSentinelPool jedisSentinelPool) {
            return new RedisRegistryService(jedisSentinelPool);
        }

    }

    @ConditionalOnProperty(prefix = "hangu.rpc.registry", name = "protocol", havingValue = "zookeeper")
    @Configuration(proxyBeanMethods = false)
    public class ZookeeperRegistryConfiguration {

        @Bean
        @ConditionalOnMissingBean(ZookeeperConfigProperties.class)
        @ConfigurationProperties(prefix = "hangu.rpc.registry.zookeeper")
        public ZookeeperConfigProperties zookeeperConfigProperties() {
            ZookeeperConfigProperties properties = new ZookeeperConfigProperties();
            return properties;
        }

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean(RegistryService.class)
        public RegistryService zookeeperRegistryService(ZookeeperConfigProperties zookeeperConfigProperties) {
            return new ZookeeperRegistryService(zookeeperConfigProperties);
        }

    }
}
