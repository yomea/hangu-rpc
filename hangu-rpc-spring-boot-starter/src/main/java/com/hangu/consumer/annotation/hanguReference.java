package com.hangu.consumer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * rpc 服务引入
 *
 * @author wuzhenhong
 * @date 2023/7/31 14:57
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface hanguReference {

    String groupName() default "";

    String interfaceName() default "";

    String version() default "";
}
