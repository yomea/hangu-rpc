package com.hanggu.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2023/8/2 17:28
 */
@AllArgsConstructor
@Getter
public enum OptionTypeEnum {

    UPDATE(1, "更新"),
    ADD(2, "新增"),
    DELETE(3, "删除");

    private Integer type;

    private String desc;
}
