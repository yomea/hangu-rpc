package com.hangu.rpc.common.properties;

import com.hangu.rpc.consumer.properties.ConsumerProperties;
import com.hangu.rpc.provider.properties.ProviderProperties;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:18
 */
@Data
public class HanguProperties {

    private int coreNum;

    private int maxNum;

    private ConsumerProperties consumer;

    private ProviderProperties provider;

}
