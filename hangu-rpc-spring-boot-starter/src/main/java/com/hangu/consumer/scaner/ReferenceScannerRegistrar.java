package com.hangu.consumer.scaner;

import com.hangu.consumer.annotation.ReferenceScan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author wuzhenhong
 * @date 2023/8/2 13:27
 */
public class ReferenceScannerRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes mapperScanAttrs = AnnotationAttributes
            .fromMap(importingClassMetadata.getAnnotationAttributes(ReferenceScan.class.getName()));
        if (mapperScanAttrs != null) {
            registerBeanDefinitions(importingClassMetadata, mapperScanAttrs, registry,
                this.generateBaseBeanName(importingClassMetadata, 0));
        }
    }

    void registerBeanDefinitions(AnnotationMetadata annoMeta, AnnotationAttributes annoAttrs,
        BeanDefinitionRegistry registry, String beanName) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ReferenceScannerConfigurer.class);

        List<String> basePackages = new ArrayList<>();

        basePackages.addAll(Arrays.stream(annoAttrs.getStringArray("basePackages")).filter(StringUtils::hasText)
            .collect(Collectors.toList()));

        basePackages.addAll(Arrays.stream(annoAttrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName)
            .collect(Collectors.toList()));

        // 没有指定，那么按照使用该注解的类的路劲往下找
        if (basePackages.isEmpty()) {
            basePackages.add(getDefaultBasePackage(annoMeta));
        }

        builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));

        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());

    }

    private static String generateBaseBeanName(AnnotationMetadata importingClassMetadata, int index) {
        return importingClassMetadata.getClassName() + "#" + ReferenceScannerRegistrar.class.getSimpleName() + "#"
            + index;
    }

    private static String getDefaultBasePackage(AnnotationMetadata importingClassMetadata) {
        return ClassUtils.getPackageName(importingClassMetadata.getClassName());
    }

}
