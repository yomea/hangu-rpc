package com.hangu.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wuzhenhong
 * @date 2023/8/4 16:22
 */
@ConfigurationProperties(prefix = "hangu.rpc.registry.redis")
@Data
public class JedisConfigPropertis {

    private String nodes;
    private String master;
    private String password;

}
