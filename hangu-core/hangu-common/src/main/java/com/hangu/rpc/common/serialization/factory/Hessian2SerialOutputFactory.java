package com.hangu.rpc.common.serialization.factory;

import com.hangu.rpc.common.serialization.SerialOutput;
import com.hangu.rpc.common.serialization.SerialOutputFactory;
import com.hangu.rpc.common.serialization.impl.Hessian2SerialOutput;
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
