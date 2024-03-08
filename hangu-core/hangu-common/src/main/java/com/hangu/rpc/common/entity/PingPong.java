package com.hangu.rpc.common.entity;

import com.hangu.rpc.common.enums.SerializationTypeEnum;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/1 11:21
 */
@Data
public class PingPong {

    private Long id;

    /**
     * @see SerializationTypeEnum
     * 序列化类型
     */
    private byte serializationType;
}
