package com.hangu.provider.invoker;

import cn.hutool.json.JSONUtil;
import com.hangu.common.entity.HttpServletRequest;
import com.hangu.common.entity.Request;
import com.hangu.common.entity.Response;
import com.hangu.common.entity.RpcInvokerContext;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.exception.RpcInvokerException;
import com.hangu.common.util.CommonUtils;
import com.hangu.provider.entity.HttpParameterBinderResult;
import com.hangu.provider.resolver.MethodArgumentResolverHandler;
import io.netty.channel.ChannelHandlerContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by wuzhenhong on 2023/8/1 22:54
 */
public class RpcInvoker {

    private Object service;

    private MethodArgumentResolverHandler methodArgumentResolverHandler;

    public RpcInvoker(Object service, MethodArgumentResolverHandler methodArgumentResolverHandler) {
        if(Objects.isNull(service)) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "目标服务不能为null！");
        }
        this.service = service;
        if(Objects.isNull(methodArgumentResolverHandler)) {
            this.methodArgumentResolverHandler = MethodArgumentResolverHandler.DEFAULT_RESOLVER;
        } else {
            this.methodArgumentResolverHandler = methodArgumentResolverHandler;
        }
    }

    public void invoke(RpcInvokerContext rpcInvokerContext) {

        String methodName = rpcInvokerContext.getMethodName();
        List<Class<?>> parameterTypes = rpcInvokerContext.getParameterTypeList();
        List<Object> parameterValues = rpcInvokerContext.getParameterValueList();

        Request request = rpcInvokerContext.getRequest();
        ChannelHandlerContext ctx = rpcInvokerContext.getCtx();

        Response response;
        try {
            Class<?> clss = service.getClass();
            Class<?>[] parameterTypeArr = Optional.ofNullable(parameterTypes).orElse(Collections.emptyList())
                .toArray(new Class<?>[0]);
            Object[] parameterValuesArr = Optional.ofNullable(parameterValues).orElse(Collections.emptyList())
                .toArray(new Object[0]);
            boolean http = rpcInvokerContext.getRequest().isHttp();
            // 如果是http请求，先要将处理的参数
            if (http) {
                HttpParameterBinderResult result = this.binderHttpArgs(clss, methodName, (HttpServletRequest)parameterValues.get(0));
                parameterTypeArr = Optional.ofNullable(result.getParameterTypes()).orElse(new Class<?>[0]);
                parameterValuesArr = Optional.ofNullable(result.getParameterValues()).orElse(new Object[0]);
            }
            Method method = clss.getMethod(methodName, parameterTypeArr);
            method.setAccessible(true);
            Object result = method.invoke(service, parameterValuesArr);
            if(http) {
                response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                    ErrorCodeEnum.SUCCESS.getCode(), String.class, JSONUtil.toJsonStr(result));
            } else {
                response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                    ErrorCodeEnum.SUCCESS.getCode(), method.getReturnType(), result);
            }
        } catch (NoSuchMethodException e) {
            response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                ErrorCodeEnum.NOT_FOUND.getCode(), e.getClass(), e);
        } catch (InvocationTargetException e) {
            response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                ErrorCodeEnum.FAILURE.getCode(), e.getClass(), e);
        } catch (IllegalAccessException e) {
            response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                ErrorCodeEnum.FORBID.getCode(), e.getClass(), e);
        } catch (Exception e) {
            response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                ErrorCodeEnum.FAILURE.getCode(), e.getClass(), e);
        }

        ctx.channel().writeAndFlush(response);
    }

    private HttpParameterBinderResult binderHttpArgs(Class<?> clss, String methodName, HttpServletRequest httpServletRequest) {

        Method[] methods = clss.getMethods();
        // 目前http请求的方式不支持重载，后续有空再迭代
        Method method = Arrays.stream(methods).filter(m -> m.getName().equals(methodName)).findFirst()
            .orElseThrow(() -> new RpcInvokerException(ErrorCodeEnum.NOT_FOUND.getCode(),
                String.format("未找到服务：%s#%s", clss.getName(), methodName)));
        Parameter[] parameters = method.getParameters();
        if(Objects.isNull(parameters) || parameters.length == 0) {
            HttpParameterBinderResult result = new HttpParameterBinderResult();
            return result;
        }
        List<Object> parameterValueList = Arrays.stream(parameters)
            .map(parameter -> methodArgumentResolverHandler.resolver(parameter, httpServletRequest))
            .collect(Collectors.toList());

        HttpParameterBinderResult result = new HttpParameterBinderResult();
        result.setParameterTypes(Arrays.stream(parameters).map(Parameter::getType)
            .collect(Collectors.toList()).toArray(new Class<?>[0]));
        result.setParameterValues(parameterValueList.toArray());
        return result;
    }
}
