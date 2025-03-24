package com.hangu.rpc.common.properties;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 16:22
 */
@Data
public class JedisConfigPropertis {

    private String publicChannel;
    private String nodes;
    private String master;
    private String password;

}
