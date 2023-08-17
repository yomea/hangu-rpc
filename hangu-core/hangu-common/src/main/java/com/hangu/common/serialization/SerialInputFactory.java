package com.hangu.common.serialization;

import java.io.InputStream;

/**
 * @author wuzhenhong
 * @date 2023/8/1 16:34
 */
public interface SerialInputFactory {

    SerialInput createSerialization(InputStream inputStream);
}
