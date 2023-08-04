package com.hanggu.consumer.client;

import com.hanggu.common.entity.HostInfo;
import io.netty.channel.Channel;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 15:40
 */
@Data
public class ClientConnect {

    private Channel channel;

    private HostInfo hostInfo;
}
