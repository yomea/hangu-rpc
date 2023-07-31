package com.hanggu.common.serialization;

import java.io.IOException;

/**
 * @author wuzhenhong
 * @date 2023/7/31 17:15
 */
public interface Serialization {

    <T> byte[] serialize(T t) throws IOException;

    <T> T deserialize(byte[] bytes, Class<T> classType) throws IOException ;

}
