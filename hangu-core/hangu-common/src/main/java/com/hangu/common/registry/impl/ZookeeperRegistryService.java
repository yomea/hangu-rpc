package com.hangu.common.registry.impl;

import cn.hutool.json.JSONUtil;
import com.hangu.common.entity.HostInfo;
import com.hangu.common.entity.RegistryInfo;
import com.hangu.common.entity.RegistryNotifyInfo;
import com.hangu.common.entity.ServerInfo;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.exception.RpcInvokerException;
import com.hangu.common.exception.RpcInvokerTimeoutException;
import com.hangu.common.exception.RpcParseException;
import com.hangu.common.listener.CuratorConnectionStateListener;
import com.hangu.common.listener.RegistryNotifyListener;
import com.hangu.common.properties.ZookeeperConfigProperties;
import com.hangu.common.registry.RegistryService;
import com.hangu.common.util.CommonUtils;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * @author wuzhenhong
 * @date 2023/8/17 10:14
 */
@Slf4j
public class ZookeeperRegistryService implements RegistryService {

    private int DEFAULT_CONNECTION_TIMEOUT_MS = 5 * 1000;
    private int DEFAULT_SESSION_TIMEOUT_MS = 60 * 1000;

    private Charset CHARSET = StandardCharsets.UTF_8;


    private final CuratorFramework client;

    public ZookeeperRegistryService(ZookeeperConfigProperties properties) {
        this.argsCheck(properties);
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .connectString(properties.getHosts())
            .retryPolicy(new RetryNTimes(1, 1000))
            .connectionTimeoutMs(properties.getConnectTimeout())
            .sessionTimeoutMs(properties.getSessionTimeout());
        String username = properties.getUserName();
        String password = properties.getPassword();
        if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
            String authority = StringUtils.trimToEmpty(username) + ":" + StringUtils.trimToEmpty(password);
            builder = builder.authorization("digest", authority.getBytes());
        }
        client = builder.build();
        client.getConnectionStateListenable().addListener(new CuratorConnectionStateListener(properties));
        client.start();
        boolean connected;
        try {
            connected = client.blockUntilConnected(properties.getConnectTimeout(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RpcInvokerTimeoutException(ErrorCodeEnum.TIME_OUT.getCode(), "连接注册中心超时！", e);
        }
        if (!connected) {
            throw new IllegalStateException("zookeeper not connected");
        }
    }

    @Override
    public void register(RegistryInfo registryInfo) {
        // 创建完整的路径
        String path = this.createFullPath(registryInfo);
        this.create(path, true);
    }

    @Override
    public void unRegister(RegistryInfo serverInfo) {
        try {
            String path = this.createFullPath(serverInfo);
            this.deletePath(path);
        } catch (Throwable e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(),
                "Failed to unregister registry info + " + JSONUtil.toJsonStr(serverInfo) + ", cause: " + e.getMessage(),
                e);
        }
    }

    @Override
    public void subscribe(RegistryNotifyListener listener, ServerInfo serverInfo) {

        String servicePath = this.createServicePath(serverInfo);
        this.create(servicePath, false);
        CuratorWatcher curatorWatcher = new CuratorWatcherImpl(listener, client, servicePath,
            serverInfo);
        this.addListener(curatorWatcher, servicePath);
    }

    @Override
    public List<HostInfo> pullServers(ServerInfo serverInfo) {
        String sererPath = this.createServicePath(serverInfo);
        List<String> hosts = this.getChildren(sererPath);
        return hosts.stream().map(str -> {
            String arr[] = str.split(":");
            HostInfo hostInfo = new HostInfo();
            hostInfo.setHost(arr[0]);
            hostInfo.setPort(Integer.parseInt(arr[1]));
            return hostInfo;
        }).collect(Collectors.toList());
    }

    private void argsCheck(ZookeeperConfigProperties properties) {

        String hosts = properties.getHosts();
        if (StringUtils.isBlank(hosts)) {
            throw new RpcParseException(ErrorCodeEnum.FAILURE.getCode(), "未配置zookeeper地址！");
        }

        int timeout = properties.getConnectTimeout();
        timeout = timeout <= 0 ? DEFAULT_CONNECTION_TIMEOUT_MS : timeout;
        properties.setConnectTimeout(timeout);
        int sessionExpireMs = properties.getSessionTimeout();
        sessionExpireMs = sessionExpireMs <= 0 ? DEFAULT_SESSION_TIMEOUT_MS : sessionExpireMs;
        properties.setSessionTimeout(sessionExpireMs);
    }

    private void create(String path, boolean ephemeral) {
        // 检查持久化Znode是否存在，主要是订阅的是需要使用，订阅时可能提供者还没有启动
        // 这里需要监控该节点
        if(!ephemeral) {
            if(this.checkExists(path)) {
                return;
            }
        }
        int i = path.lastIndexOf('/');
        if (i > 0) {
            create(path.substring(0, i), false);
        }
        if (ephemeral) {
            createEphemeral(path);
        } else {
            createPersistent(path);
        }
    }

    private void createPersistent(String path) {
        try {
            client.create().forPath(path);
        } catch (NodeExistsException e) {
            log.warn("ZNode " + path + " already exists.", e);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void createEphemeral(String path) {
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path);
        } catch (NodeExistsException e) {
            log.warn(
                "ZNode " + path + " already exists, 对于这种情况可能是其他的session在删除，但是还没来得及删，这里尝试在当前session里先删除再创建", e);
            deletePath(path);
            createEphemeral(path);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void createPersistent(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            try {
                client.setData().forPath(path, dataBytes);
            } catch (Exception e1) {
                throw new IllegalStateException(e.getMessage(), e1);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void createEphemeral(String path, String data) {
        byte[] dataBytes = data.getBytes(CHARSET);
        try {
            client.create().withMode(CreateMode.EPHEMERAL).forPath(path, dataBytes);
        } catch (NodeExistsException e) {
            log.warn(
                "ZNode " + path + " already exists, 对于这种情况可能是其他的session在删除，但是还没来得及删，这里尝试在当前session里先删除再创建", e);
            deletePath(path);
            createEphemeral(path, data);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private void deletePath(String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (NoNodeException ignored) {
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private List<String> getChildren(String path) {
        try {
            // 获取指定路径下的所有目录
            return client.getChildren().forPath(path);
        } catch (NoNodeException e) {
            return Collections.emptyList();
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private boolean checkExists(String path) {
        try {
            if (client.checkExists().forPath(path) != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isConnected() {
        return client.getZookeeperClient().isConnected();
    }

    private String getContent(String path) {
        try {
            byte[] dataBytes = client.getData().forPath(path);
            return (dataBytes == null || dataBytes.length == 0) ? null : new String(dataBytes, CHARSET);
        } catch (NoNodeException e) {
            // 忽略不存在该节点异常
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return null;
    }

    private void close() {
        client.close();
    }

    private String createServicePath(ServerInfo serverInfo) {
        return "/" + CommonUtils.createServiceKey(serverInfo);
    }

    private String createFullPath(RegistryInfo registryInfo) {

        return this.createServicePath(registryInfo) + "/" + registryInfo.getHostInfo().toString();
    }

    private List<String> addListener(CuratorWatcher curatorWatcher, String servicePath) {
        try {
            return client.getChildren().usingWatcher(curatorWatcher).forPath(servicePath);
        } catch (NoNodeException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private static class CuratorWatcherImpl implements CuratorWatcher {

        private static final ExecutorService CURATOR_WATCHER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "CuratorWatcherImpl - thread");
            return thread;
        });
        private RegistryNotifyListener listener;
        private CuratorFramework client;

        private String servicePath;

        private ServerInfo serverInfo;

        public CuratorWatcherImpl(RegistryNotifyListener listener, CuratorFramework client, String servicePath,
            ServerInfo serverInfo) {
            this.listener = listener;
            this.client = client;
            this.servicePath = servicePath;
            this.serverInfo = serverInfo;
        }


        @Override
        public void process(WatchedEvent event) throws Exception {

            if (event.getType() == Watcher.Event.EventType.None) {
                return;
            }

            if (listener != null) {
                Runnable task = () -> {
                    try {

                        List<String> hosts = client.getChildren().usingWatcher(CuratorWatcherImpl.this)
                            .forPath(servicePath);
                        List<HostInfo> hostInfoList = hosts.stream().map(str -> {
                            String arr[] = str.split(":");
                            HostInfo hostInfo = new HostInfo();
                            hostInfo.setHost(arr[0]);
                            hostInfo.setPort(Integer.parseInt(arr[1]));
                            return hostInfo;
                        }).collect(Collectors.toList());
                        RegistryNotifyInfo notifyInfo = new RegistryNotifyInfo();
                        notifyInfo.setServerInfo(serverInfo);
                        notifyInfo.setHostInfos(hostInfoList);
                        listener.registryNotify(notifyInfo);
                    } catch (Exception e) {
                        log.warn("获取服务 serverInfo => {} 下的提供者地址失败！", JSONUtil.toJsonStr(serverInfo), e);
                    }
                };

                CURATOR_WATCHER_EXECUTOR.execute(task);
            }
        }
    }
}
