package com.hanggu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/1 11:21
 */
@Data
public class PingPong {

    private Long id;

    /**
     * @see com.hanggu.common.enums.SerializationTypeEnum
     * 序列化类型
     */
    private byte serializationType;
}
