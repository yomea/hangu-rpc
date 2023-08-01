package com.hanggu.common.entity;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author wuzhenhong
 * @date 2023/8/1 13:21
 */
@Data
public class RpcRequestTransport implements Serializable {

    /**
     * 组名：可以是应用名，表示该应用下的某个接口
     * 用于区分多个应用，接口名冲突的场景
     */
    private String groupName;

    /**
     * 接口名
     */
    private String interfaceName;

    /**
     * 版本号
     */
    private String version;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数类型字节码描述符与对应的值
     */
    private List<ParameterInfo> parameterInfos;
}
