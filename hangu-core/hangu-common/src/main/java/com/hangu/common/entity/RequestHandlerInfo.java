package com.hangu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2024/2/7 15:51
 */
@Data
public class RequestHandlerInfo {

    /**
     * true：http 请求
     * false：普通的 rpc 请求
     */
    private boolean http;

    /**
     * 如果是http，那么要调用的方法名是外部传递进来的
     */
    private String methodName;

    /**
     * 服务信息
     */
    private ServerInfo serverInfo;
}
