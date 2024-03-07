package com.hangu.consumer.client;

import com.hangu.common.entity.HostInfo;
import io.netty.channel.Channel;
import java.util.Objects;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:40
 */
@Data
public class ClientConnect {

    private volatile Channel channel;

    private HostInfo hostInfo;

    private int retryConnectCount;

    /**
     * 标记为释放，接收到注册中心的通知，次机器下线，这个值设置为true
     */
    private volatile boolean release;

    public ClientConnect(Channel channel, HostInfo hostInfo) {
        this.channel = channel;
        this.hostInfo = hostInfo;
    }

    public void updateChannel(Channel channel) {
        this.channel = channel;
    }

    public int incrConnCount() {
        this.retryConnectCount++;
        // 超过20次了，标记为要释放掉，确实没法连上了
        if (this.retryConnectCount > 20) {
            this.markRelease();
        }
        return this.retryConnectCount;
    }

    public void resetConnCount() {
        this.retryConnectCount = 0;
    }

    public boolean isActive() {
        return Objects.nonNull(this.channel) && this.channel.isActive();
    }

    public void markRelease() {
        this.release = true;
    }

    public boolean isRelease() {
        return release;
    }
}
