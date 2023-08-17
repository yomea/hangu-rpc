package com.hangu.common.properties;

import com.hangu.consumer.properties.ConsumerProperties;
import com.hangu.provider.properties.ProviderProperties;
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
