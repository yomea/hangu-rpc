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

    public void updateChannel(Channel channel) {
        this.channel = channel;
    }

    public int incrConnCount() {
        return this.retryConnectCount++;
    }

    public void resetConnCount() {
        this.retryConnectCount = 0;
    }

    public boolean isActive() {
        return Objects.nonNull(this.channel) && this.channel.isActive();
    }

    public boolean isRelease() {
        return retryConnectCount > 20;
    }
}
