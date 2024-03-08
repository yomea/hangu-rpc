package com.hangu.rpc.common.exception;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:19
 */
public class UnSupportSerialTypeException extends RuntimeException {

    public UnSupportSerialTypeException(String msg) {
        super(msg);
    }

    public UnSupportSerialTypeException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
