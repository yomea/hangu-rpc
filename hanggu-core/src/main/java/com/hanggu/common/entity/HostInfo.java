package com.hanggu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:13
 */
@Data
public class HostInfo {


    /**
     * @see com.hanggu.common.enums.OptionTypeEnum
     */
    private Integer option;

    private String host;

    private int port;

}
