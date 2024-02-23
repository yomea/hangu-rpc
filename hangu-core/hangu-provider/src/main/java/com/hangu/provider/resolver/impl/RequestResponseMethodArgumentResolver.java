package com.hangu.provider.resolver.impl;

import com.hangu.common.entity.HttpServletRequest;
import com.hangu.common.entity.HttpServletResponse;
import com.hangu.provider.resolver.MethodArgumentResolver;
import java.lang.reflect.Parameter;
import org.springframework.core.Ordered;

/**
 * @author wuzhenhong
 * @date 2024/2/19 15:29
 */
public class RequestResponseMethodArgumentResolver implements MethodArgumentResolver, Ordered {

    @Override
    public boolean support(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        Class<?> clazz = parameter.getType();
        return clazz == HttpServletRequest.class || clazz == HttpServletResponse.class;
    }

    @Override
    public Object resolver(Parameter parameter, HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse) {
        Class<?> clazz = parameter.getType();
        return clazz == HttpServletRequest.class
            ? httpServletRequest
            : httpServletResponse;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
