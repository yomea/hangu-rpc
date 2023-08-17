package com.hangu.common.properties;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/17 18:32
 */
@Data
public class ZookeeperConfigProperties {

    private int connectTimeout;
    private int sessionTimeout;
    private String hosts;
    private String userName;
    private String password;
}
