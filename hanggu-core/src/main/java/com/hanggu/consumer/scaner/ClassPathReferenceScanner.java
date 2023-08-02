package com.hanggu.consumer.scaner;

import com.hanggu.consumer.annotation.HangguReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:01
 */
public class ClassPathReferenceScanner extends ClassPathBeanDefinitionScanner {

    private Class<?> factoryBean;

    public ClassPathReferenceScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }

    public Class<?> getFactoryBean() {
        return factoryBean;
    }

    public void setFactoryBean(Class<?> factoryBean) {
        this.factoryBean = factoryBean;
    }

    public void registerFilters() {
        super.addIncludeFilter(new AnnotationTypeFilter(HangguReference.class));
    }
}
