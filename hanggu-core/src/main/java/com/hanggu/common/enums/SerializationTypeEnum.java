package com.hanggu.common.enums;

import com.hanggu.common.exception.UnSupportSerialTypeException;
import com.hanggu.common.serialization.SerialInput;
import com.hanggu.common.serialization.SerialInputFactory;
import com.hanggu.common.serialization.SerialOutput;
import com.hanggu.common.serialization.SerialOutputFactory;
import com.hanggu.common.serialization.factory.Hessian2SerialInputFactory;
import com.hanggu.common.serialization.factory.Hessian2SerialOutputFactory;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:10
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {

    HESSIAN((byte) 1, "hessian", new Hessian2SerialOutputFactory(), new Hessian2SerialInputFactory()),
//    JDK((byte) 2, "jdk", new JdkSerializer()),
//    JSON((byte) 3, "json", new JsonSerializer())
    ;

    private byte type;

    private String name;

    private SerialOutputFactory serialOutputFactory;
    private SerialInputFactory serialInputFactory;

    public static SerialOutput getOutputByType(OutputStream outputStream, byte type) {
        for (SerializationTypeEnum typeEnum : values()) {
            if (typeEnum.type == type) {
                return typeEnum.getSerialOutputFactory().createSerialization(outputStream);
            }
        }
        throw new UnSupportSerialTypeException(String.format("类型为%s的序列化不支持！", type));
    }

    public static SerialInput getInputByType(InputStream inputStream, byte type) {
        for (SerializationTypeEnum typeEnum : values()) {
            if (typeEnum.type == type) {
                return typeEnum.getSerialInputFactory().createSerialization(inputStream);
            }
        }
        throw new UnSupportSerialTypeException(String.format("类型为%s的序列化不支持！", type));
    }
}
