package com.hangu.rpc.provider.resolver.impl;

import com.hangu.rpc.common.entity.HttpServletRequest;
import com.hangu.rpc.common.entity.HttpServletResponse;
import com.hangu.rpc.provider.binder.WebDataBinder;
import com.hangu.rpc.provider.resolver.AbstractMethodArgumentResolver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;

/**
 * @author wuzhenhong
 * @date 2024/2/8 14:34
 */
public class FormDataMethodArgumentResolver extends AbstractMethodArgumentResolver {

    public FormDataMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public boolean support(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        String contentType = StringUtils.trimToEmpty(httpServletRequest.getHeads().get("content-type"));
        Class<?> type = parameter.getType();
        return !ClassUtils.isPrimitiveOrWrapper(type)
            && type != String.class
            && !contentType.startsWith(
            "application/json");
    }

    @Override
    public Object resolver(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {

        Constructor<?> ctor = BeanUtils.getResolvableConstructor(parameter.getType());
        Object target = BeanUtils.instantiateClass(ctor);
        MutablePropertyValues mpvs = new MutablePropertyValues();
        Map<String, String[]> getParam = Optional.ofNullable(httpServletRequest.getGetParam())
            .orElse(Collections.emptyMap());
        getParam.forEach((k, v) -> {
            Arrays.stream(v).forEach(value -> {
                mpvs.add(k, value);
            });
        });
        WebDataBinder webDataBinder = new WebDataBinder(target, conversionService);
        webDataBinder.bind(mpvs);
        return target;
    }
}
