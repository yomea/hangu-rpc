package com.hanggu.common.entity;

import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:21
 */
@Data
public class RegistryNotifyInfo {

    /**
     * 客户端订阅的组
     */
    private String groupName;

    /**
     * 发生变更的明细
     */
    private List<RegistryNotifyItem> itemList;

}
