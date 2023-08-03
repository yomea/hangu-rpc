package com.hanggu.common.entity;

import java.io.Serializable;
import lombok.Data;

/**
 * 请求响应
 *
 * @author wuzhenhong
 * @date 2023/8/1 11:00
 */
@Data
public class Response implements Serializable {

    private Long id;

    /**
     * @see com.hanggu.common.enums.SerializationTypeEnum
     * 消息类型
     */
    private byte serializationType;

    private RpcResponseTransport rpcResponseTransport;
}
