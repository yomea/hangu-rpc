package com.hanggu.consumer.scaner;

import com.hanggu.consumer.factory.ReferenceFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * 扫描并配置rpc接口
 *
 * @author wuzhenhong
 * @date 2023/8/2 8:58
 */
public class ReferenceScannerConfigurer implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware,
    EnvironmentAware {

    private String basePackage;

    private ApplicationContext applicationContext;
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        ClassPathReferenceScanner scanner = new ClassPathReferenceScanner(registry);
        scanner.setResourceLoader(this.applicationContext);
        scanner.setFactoryBean(ReferenceFactoryBean.class);
        scanner.setEnvironment(environment);
        scanner.registerFilters();
        scanner.scan(
            StringUtils.tokenizeToStringArray(this.basePackage,
                ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
