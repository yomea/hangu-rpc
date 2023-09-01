package com.hangu.common.annotation;

import com.hangu.common.configuration.HanguAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by wuzhenhong on 2023/9/1 10:09
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(HanguAutoConfiguration.class)
public @interface EnableHanguRpc {
}
