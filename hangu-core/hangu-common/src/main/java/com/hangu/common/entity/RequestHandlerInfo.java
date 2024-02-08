package com.hangu.common.entity;

import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2024/2/7 15:51
 */
@Data
public class RequestHandlerInfo {

    /**
     * 服务信息
     */
    private ServerInfo serverInfo;

    /**
     * 用户自己提供的指定方法调用方法信息，如果未提供，从给定接口方法方法解析
     */
    private MethodInfo providedMethodInfo;
}
