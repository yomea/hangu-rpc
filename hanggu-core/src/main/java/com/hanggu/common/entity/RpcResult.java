package com.hanggu.common.entity;

import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/1 23:07
 */
@Data
public class RpcResult {

    /**
     * @see com.hanggu.common.enums.ErrorCodeEnum
     */
    private int code;

    private Class<?> returnType;

    private Object result;
}
