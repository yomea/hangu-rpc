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
     * 方法名
     */
    private String name;

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

    private Method method;
}
