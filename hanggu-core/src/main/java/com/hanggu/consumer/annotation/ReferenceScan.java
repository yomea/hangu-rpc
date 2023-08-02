package com.hanggu.consumer.annotation;

import com.hanggu.consumer.scaner.ReferenceScannerRegistrar;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:27
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(ReferenceScannerRegistrar.class)
public @interface ReferenceScan {

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};
}
