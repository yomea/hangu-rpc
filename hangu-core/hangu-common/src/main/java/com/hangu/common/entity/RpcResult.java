package com.hangu.common.entity;

import com.hangu.common.enums.ErrorCodeEnum;
import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/1 23:07
 */
@Data
public class RpcResult {

    /**
     * @see ErrorCodeEnum
     */
    private int code;

    private Class<?> returnType;

    private Object result;
}
