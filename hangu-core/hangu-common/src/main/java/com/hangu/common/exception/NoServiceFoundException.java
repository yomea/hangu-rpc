package com.hangu.common.exception;

/**
 * @author wuzhenhong
 * @date 2023/8/1 14:19
 */
public class NoServiceFoundException extends RuntimeException {

    public NoServiceFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public NoServiceFoundException(String msg) {
        super(msg);
    }
}


