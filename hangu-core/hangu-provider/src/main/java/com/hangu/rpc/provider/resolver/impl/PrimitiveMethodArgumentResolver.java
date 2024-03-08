package com.hangu.rpc.provider.resolver.impl;

import com.hangu.rpc.common.entity.HttpServletRequest;
import com.hangu.rpc.common.entity.HttpServletResponse;
import com.hangu.rpc.common.util.DescClassUtils;
import com.hangu.rpc.provider.resolver.AbstractMethodArgumentResolver;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.core.convert.ConversionService;

/**
 * @author wuzhenhong
 * @date 2024/2/8 14:34
 */
public class PrimitiveMethodArgumentResolver extends AbstractMethodArgumentResolver {

    public PrimitiveMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public boolean support(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        Class<?> type = parameter.getType();
        return ClassUtils.isPrimitiveOrWrapper(type) || type == String.class;
    }

    @Override
    public Object resolver(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        String name = parameter.getName();
        Class<?> type = parameter.getType();
        Map<String, String[]> getParam = Optional.ofNullable(httpServletRequest.getGetParam())
            .orElse(Collections.emptyMap());
        String[] values = getParam.get(name);
        if (Objects.isNull(values) || values.length == 0) {
            return DescClassUtils.getInitPrimitiveValue(type);
        } else {
            String v = values[0];
            return conversionService.convert(v, type);
        }
    }
}
