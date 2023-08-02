package com.hanggu.consumer.manager;

import com.hanggu.common.entity.HostInfo;
import com.hanggu.common.entity.RegistryNotifyInfo;
import com.hanggu.consumer.listener.RegistryNotifyListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author wuzhenhong
 * @date 2023/8/2 16:56
 */
public class RegistryDirectory implements RegistryNotifyListener {

    private String groupName;

    private String interfaceName;

    private String version;
    /**
     * 服务提供者地址
     */
    private Map<String, List<HostInfo>> invokersMap;

    public RegistryDirectory() {
        // TODO: 2023/8/2 初始化拉取服务
        this.initPullService();
    }

    private void initPullService() {
        // TODO: 2023/8/2 拉取服务
    }

    public List<HostInfo> getByKey(String key) {
        return invokersMap.getOrDefault(key, Collections.emptyList());
    }

    @Override
    public void notify(RegistryNotifyInfo notifyInfo) {
        // 刷新本地服务列表
    }
}
