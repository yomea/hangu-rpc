package com.hanggu.common.serialization.impl;

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import com.hanggu.common.serialization.SerialOutput;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:17
 */
public class Hessian2SerialOutput implements SerialOutput {

    private Hessian2Output output;

    public Hessian2SerialOutput(OutputStream outputStream) {
        output = new Hessian2Output(outputStream);
        output.setSerializerFactory(new SerializerFactory());
    }

    @Override
    public void writeObject(Object obj) throws IOException {
        output.writeObject(obj);
    }

    @Override
    public void writeString(String text) throws IOException {
        output.writeString(text);
    }
}
