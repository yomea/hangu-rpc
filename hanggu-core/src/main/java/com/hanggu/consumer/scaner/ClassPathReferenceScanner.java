package com.hanggu.consumer.scaner;

import com.hanggu.consumer.annotation.HangguReference;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * @author wuzhenhong
 * @date 2023/8/2 9:01
 */
@Slf4j
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
        AnnotationTypeFilter referenceFilter = new AnnotationTypeFilter(HangguReference.class, true, false);
        super.addIncludeFilter(referenceFilter);
    }

    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.isEmpty()) {
            log.warn("No reference service was found in '" + Arrays.toString(basePackages)
                + "' package. Please check your configuration.");
        } else {
            this.processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions;
    }

    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        AbstractBeanDefinition definition;
        BeanDefinitionRegistry registry = getRegistry();
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (AbstractBeanDefinition) holder.getBeanDefinition();
            boolean scopedProxy = false;
            if (ScopedProxyFactoryBean.class.getName().equals(definition.getBeanClassName())) {
                definition = (AbstractBeanDefinition) Optional
                    .ofNullable(((RootBeanDefinition) definition).getDecoratedDefinition())
                    .map(BeanDefinitionHolder::getBeanDefinition).orElseThrow(() -> new IllegalStateException(
                        "The target bean definition of scoped proxy bean not found. Root bean definition[" + holder
                            + "]"));
                scopedProxy = true;
            }
            String beanClassName = definition.getBeanClassName();
            // 添加构造参数
            definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
            // 将该 bean 定义为工厂bean
            definition.setBeanClass(this.factoryBean);

            if (scopedProxy) {
                continue;
            }

            // 如果当前扫描出来的bean不是单例，那么进行范围代理，保持多例
            if (!definition.isSingleton()) {
                BeanDefinitionHolder proxyHolder = ScopedProxyUtils.createScopedProxy(holder, registry, true);
                if (registry.containsBeanDefinition(proxyHolder.getBeanName())) {
                    registry.removeBeanDefinition(proxyHolder.getBeanName());
                }
                registry.registerBeanDefinition(proxyHolder.getBeanName(), proxyHolder.getBeanDefinition());
            }

        }
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }
}
