package com.hangu.common.entity;

import com.hangu.common.enums.SerializationTypeEnum;
import java.io.Serializable;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:29
 */
@Data
public class Request implements Serializable {

    /**
     * 请求ID，8字节
     */
    private Long id;

    /**
     * @see SerializationTypeEnum
     * 序列化类型
     */
    private byte serializationType;

    private RpcRequestTransport invokerTransport;
}
