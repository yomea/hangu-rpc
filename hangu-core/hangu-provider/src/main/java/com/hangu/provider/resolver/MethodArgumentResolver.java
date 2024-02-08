package com.hangu.provider.resolver;

import com.hangu.common.entity.HttpServletRequest;
import java.lang.reflect.Parameter;

/**
 * @author wuzhenhong
 * @date 2024/2/8 14:31
 */
public interface MethodArgumentResolver {

    boolean support(Parameter parameter, HttpServletRequest httpServletRequest);

    Object resolver(Parameter parameter, HttpServletRequest httpServletRequest);
}
