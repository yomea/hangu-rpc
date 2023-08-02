package com.hanggu.common.entity;

import java.util.List;
import lombok.Data;

/**
 *
 * 注册通知明细
 * @author wuzhenhong
 * @date 2023/8/2 17:23
 */
@Data
public class RegistryNotifyItem {

    /**
     * 接口名
     */
    private String interfaceName;

    /**
     * 版本
     */
    private String version;

    /**
     * 提供者地址（更新，新增，删除）
     */
    private List<HostInfo> hostInfos;

}
