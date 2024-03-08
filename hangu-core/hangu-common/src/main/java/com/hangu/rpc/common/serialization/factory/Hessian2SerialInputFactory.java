package com.hangu.rpc.common.serialization.factory;

import com.hangu.rpc.common.serialization.SerialInput;
import com.hangu.rpc.common.serialization.SerialInputFactory;
import com.hangu.rpc.common.serialization.impl.Hessian2SerialInput;
import java.io.InputStream;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:31
 */
public class Hessian2SerialInputFactory implements SerialInputFactory {

    @Override
    public SerialInput createSerialization(InputStream inputStream) {

        Hessian2SerialInput input = new Hessian2SerialInput(inputStream);
        return input;
    }
}
