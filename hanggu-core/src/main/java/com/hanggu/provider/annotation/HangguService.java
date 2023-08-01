package com.hanggu.provider.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记提供 rpc 服务
 * @author wuzhenhong
 * @date 2023/7/31 14:52
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HangguService {

    /**
     * 组名
     */
    String groupName() default "";

    /**
     *
     * 接口名，可以自定义，如果为空，那么将自动使用该类继承的接口作为名字
     */
    String interfaceName() default "";


    /**
     *
     * 版本号
     */
    String version() default "";


}
