package com.hangu.rpc.common.enums;

import com.hangu.rpc.common.exception.UnSupportSerialTypeException;
import com.hangu.rpc.common.serialization.SerialInput;
import com.hangu.rpc.common.serialization.SerialInputFactory;
import com.hangu.rpc.common.serialization.SerialOutput;
import com.hangu.rpc.common.serialization.SerialOutputFactory;
import com.hangu.rpc.common.serialization.factory.Hessian2SerialInputFactory;
import com.hangu.rpc.common.serialization.factory.Hessian2SerialOutputFactory;
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
