package com.hangu.common.handler;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:30
 */
public class ByteFrameDecoder extends LengthFieldBasedFrameDecoder {

    public ByteFrameDecoder() {
        super(2048, 11, 4, 0, 0);
    }
}
