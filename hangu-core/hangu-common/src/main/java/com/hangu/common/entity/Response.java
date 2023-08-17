package com.hangu.common.entity;

import com.hangu.common.enums.SerializationTypeEnum;
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
     * @see SerializationTypeEnum
     * 消息类型
     */
    private byte serializationType;

    private RpcResponseTransport rpcResponseTransport;
}
