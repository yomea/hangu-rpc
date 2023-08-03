package com.hanggu.common.properties;

import com.hanggu.consumer.properties.ConsumerProperties;
import com.hanggu.provider.properties.ProviderProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/3 18:18
 */
@ConfigurationProperties(prefix = "hangu.rpc")
@Data
public class HanguProperties {

    private int coreNum;

    private int maxNum;

    private ConsumerProperties consumer;

    private ProviderProperties provider;

}
