package com.hanggu.common.entity;

import com.hanggu.common.enums.MsgTypeMarkEnum;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/7/31 15:29
 */
@Data
public class HangguProtocol {

    /**
     * 魔数
     */
    private byte[] magic;

    /**
     * @see MsgTypeMarkEnum
     * 消息类型
     */
    private Integer msgType;

    /**
     * @see com.hanggu.common.enums.SerializationTypeEnum
     * 消息类型
     */
    private byte serializationType;

    /**
     * 内容长度
     */
    private int bodyLength;

    private Object msg;

}
