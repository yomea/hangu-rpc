package com.hangu.provider.resolver.impl;

import cn.hutool.json.JSONUtil;
import com.hangu.common.entity.HttpServletRequest;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.exception.RpcInvokerException;
import com.hangu.provider.resolver.MethodArgumentResolver;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Parameter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author wuzhenhong
 * @date 2024/2/8 14:34
 */
public class JsonMethodArgumentResolver implements MethodArgumentResolver {

    @Override
    public boolean support(Parameter parameter, HttpServletRequest httpServletRequest) {
        String contentType = StringUtils.trimToEmpty(httpServletRequest.getHeads().get("content-type"));
        Class<?> type = parameter.getType();
        byte[] bodyData = httpServletRequest.getBodyData();
        return !ClassUtils.isPrimitiveOrWrapper(type)
            && type != String.class
            && contentType.startsWith(
            "application/json")
            && bodyData != null && bodyData.length > 0;
    }

    @Override
    public Object resolver(Parameter parameter, HttpServletRequest httpServletRequest) {
        byte[] bodyData = httpServletRequest.getBodyData();
        try {
            String jsonData = new String(bodyData, "UTF-8");
            return JSONUtil.toBean(jsonData, parameter.getType());
        } catch (UnsupportedEncodingException e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "http请求体编码错误！",e);
        } catch (Exception e) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "http请求体json解析错误！",e);
        }
    }
}
