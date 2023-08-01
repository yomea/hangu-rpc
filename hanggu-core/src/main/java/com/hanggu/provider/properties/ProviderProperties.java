package com.hanggu.provider.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by wuzhenhong on 2023/8/1 23:49
 */
@ConfigurationProperties(prefix = "hangu")
@Data
public class ProviderProperties {

    private int port;

    private int coreNum;

    private int maxNum;
}
