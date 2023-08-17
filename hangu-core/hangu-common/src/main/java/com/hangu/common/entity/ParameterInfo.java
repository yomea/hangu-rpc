package com.hangu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:42
 */
@Data
public class ParameterInfo implements Serializable {

    /**
     * 参数下标
     */
    private int index;

    /**
     * 参数类型
     */
    private Class<?> type;

    /**
     * 参数值
     */
    private Object value;
}
