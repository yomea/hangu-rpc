package com.hanggu.common.serialization.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.hanggu.common.serialization.Serialization;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:17
 */
public class Hessian2Serialization implements Serialization {

    @Override
    public <T> byte[] serialize(T t) throws IOException {
        if(Objects.isNull(t)) {
            return new byte[0];
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(byteArrayOutputStream);
        try {
            hessian2Output.setSerializerFactory(new SerializerFactory());
            hessian2Output.writeObject(t);
        } finally {
            byteArrayOutputStream.close();
            hessian2Output.close();
        }
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Hessian2Input hessianInput = new Hessian2Input(byteArrayInputStream);
        try {
            hessianInput.setSerializerFactory(new SerializerFactory());
            return (T) hessianInput.readObject(classType);
        } finally {
            byteArrayInputStream.close();
            hessianInput.close();
        }
    }
}
