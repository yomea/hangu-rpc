package com.hanggu.common.enums;

import com.hanggu.common.serialization.Serialization;
import com.hanggu.common.serialization.impl.Hessian2Serialization;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:10
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {

    HESSIAN((byte) 1, "hessian", new Hessian2Serialization()),
//    JDK((byte) 2, "jdk", new JdkSerializer()),
//    JSON((byte) 3, "json", new JsonSerializer())
    ;

    private byte type;

    private String name;

    private Serialization serialization;

    public static Serialization getSerializationByType(byte type) {
        for (SerializationTypeEnum typeEnum : values()) {
            if (typeEnum.type == type) {
                return typeEnum.getSerialization();
            }
        }
        throw new RuntimeException("不支持的序列化类型！");
    }
}
