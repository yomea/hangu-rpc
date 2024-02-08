package com.hangu.common.entity;

import com.hangu.common.callback.RpcResponseCallback;
import com.hangu.common.enums.MethodCallTypeEnum;
import java.lang.reflect.Method;
import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/3 16:26
 */
@Data
public class MethodInfo {

    /**
     * 使用了哪种方式去调用该方法，目前就两种
     * true：客户端是通过 http 访问的
     * false：客户端是按照正常 rpc 调用的
     */
    private boolean http;

    /**
     * 方法名
     */
    private String name;

    /**
     * 方法签名 methodName(参数)
     */
    private String sign;

    /**
     * 实际参数类型
     */
    private List<ParameterInfo> factParameterInfoList;

    /**
     * 回调参数类型
     */
    private List<ParameterInfo> callbackParameterInfoList;

    /**
     * 调用超时时间
     */
    private int timeout;

    /**
     * @see MethodCallTypeEnum
     */
    private Integer callType;
    /**
     * 回调函数
     */
    private RpcResponseCallback callback;
}
