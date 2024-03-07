package com.hangu.common.annotation;

import com.hangu.common.configuration.HanguAutoConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Created by wuzhenhong on 2023/9/1 10:09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(HanguAutoConfiguration.class)
public @interface EnableHanguRpc {

}
