package com.hangu.common.exception;

import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/2 00:22
 */
@Data
public class RpcParseException extends RuntimeException {

    private int code;

    public RpcParseException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public RpcParseException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }
}
