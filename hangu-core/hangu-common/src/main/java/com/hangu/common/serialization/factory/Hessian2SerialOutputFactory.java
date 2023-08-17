package com.hangu.common.serialization.factory;

import com.hangu.common.serialization.SerialOutput;
import com.hangu.common.serialization.SerialOutputFactory;
import com.hangu.common.serialization.impl.Hessian2SerialOutput;
import java.io.OutputStream;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:31
 */
public class Hessian2SerialOutputFactory implements SerialOutputFactory {

    @Override
    public SerialOutput createSerialization(OutputStream outputStream) {

        Hessian2SerialOutput hessian2SerialOutput = new Hessian2SerialOutput(outputStream);

        return hessian2SerialOutput;
    }
}
