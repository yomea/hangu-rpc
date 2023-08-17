package com.hangu.common.registry.impl;

import com.hangu.common.entity.HostInfo;
import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.listener.RegistryNotifyListener;
import com.hangu.common.registry.RegistryService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2023/8/17 10:14
 */
@Slf4j
public class ZookeeperRegistryService implements RegistryService {

//    private final CuratorFramework client;

    public ZookeeperRegistryService() {

        /*try {
            int timeout = url.getParameter(TIMEOUT_KEY, DEFAULT_CONNECTION_TIMEOUT_MS);
            int sessionExpireMs = url.getParameter(ZK_SESSION_EXPIRE_KEY, DEFAULT_SESSION_TIMEOUT_MS);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(url.getBackupAddress())
                .retryPolicy(new RetryNTimes(1, 1000))
                .connectionTimeoutMs(timeout)
                .sessionTimeoutMs(sessionExpireMs);
            String authority = url.getAuthority();
            if (authority != null && authority.length() > 0) {
                builder = builder.authorization("digest", authority.getBytes());
            }
            client = builder.build();
            client.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(url));
            client.start();
            boolean connected = client.blockUntilConnected(timeout, TimeUnit.MILLISECONDS);
            if (!connected) {
                throw new IllegalStateException("zookeeper not connected");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }*/
    }

    @Override
    public void register(RegistryInfo registryInfo) {

    }

    @Override
    public void unRegister(RegistryInfo serverInfo) {

    }

    @Override
    public void subscribe(RegistryNotifyListener listener, ServerInfo serverInfo) {

    }

    @Override
    public List<HostInfo> pullServers(ServerInfo serverInfo) {
        return null;
    }
}
