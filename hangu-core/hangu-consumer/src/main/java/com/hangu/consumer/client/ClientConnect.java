package com.hangu.consumer.client;

import com.hangu.common.entity.HostInfo;
import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:40
 */
@Data
public class ClientConnect {

    private volatile Channel channel;

    private HostInfo hostInfo;

    public void updateChannel(Channel channel) {
        this.channel = channel;
    }
}
