package com.hangu.rpc.common.serialization;

import java.io.OutputStream;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:34
 */
public interface SerialOutputFactory {

    SerialOutput createSerialization(OutputStream outputStream);
}
