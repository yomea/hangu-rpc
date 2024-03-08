package com.hangu.rpc.common.entity;

import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import lombok.Data;

/**
 * Created by wuzhenhong on 2023/8/1 23:14
 */
@Data
public class RpcInvokerContext {

    /**
     * 请求参数
     */
    private Request request;

    /**
     * 通道处理上下文
     */
    private ChannelHandlerContext ctx;

    /**
     * 要调用的方法名
     */
    private String methodName;

    /**
     * 方法参数类型
     */
    private List<Class<?>> parameterTypeList;

    /**
     * 方法参数值
     */
    private List<Object> parameterValueList;
}
