package com.hanggu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:28
 */
@Data
public class RpcResponseTransport implements Serializable {

    // 响应编码
    private int code;

    // 返回值的类型描述
    private Class<?> type;

    // 响应体内容
    private Object vale;
}
