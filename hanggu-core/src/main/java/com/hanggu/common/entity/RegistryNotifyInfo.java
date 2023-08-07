package com.hanggu.common.entity;

import java.util.List;
import lombok.Data;

/**
 * 注册通知明细
 *
 * @author wuzhenhong
 * @date 2023/8/2 17:23
 */
@Data
public class RegistryNotifyInfo {

    private ServerInfo serverInfo;

    private List<HostInfo> hostInfos;

}
