package com.hanggu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/4 14:44
 */
@Data
public class RegistryInfo {

    private String groupName;

    private String interfaceName;

    private String version;

    private HostInfo hostInfo;
}
