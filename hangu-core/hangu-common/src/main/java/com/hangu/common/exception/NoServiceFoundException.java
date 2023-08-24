package com.hangu.common.exception;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:19
 */
public class NoServiceFoundException extends RpcInvokerException {

    public NoServiceFoundException(int code, String msg, Throwable cause) {
        super(code, msg, cause);
    }

    public NoServiceFoundException(int code, String msg) {
        super(code, msg);
    }
}


