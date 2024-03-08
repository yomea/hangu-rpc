package com.hangu.rpc.demo.exception;

import cn.hutool.json.JSONUtil;
import com.hangu.rpc.common.exception.RpcInvokerException;
import com.hangu.rpc.common.exception.RpcParseException;
import com.hangu.rpc.common.exception.UnSupportSerialTypeException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author wuzhenhong
 * @date 2023/8/10 15:49
 */
@Component
@Slf4j
public class DefaultHandlerExceptionResolver implements HandlerExceptionResolver, PriorityOrdered {

    @Override
    public int getOrder() {
        return PriorityOrdered.HIGHEST_PRECEDENCE;
    }

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
        Exception ex) {
        log.error(ex.getMessage(), ex);
        response.setContentType("application/json; charset=UTF-8");
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("code", 500);
            map.put("message", "系统开了个小差，请联系管理员处理！");
            this.dealException(ex, map);
            response.getWriter().write(JSONUtil.toJsonStr(map));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ModelAndView();
    }

    private void dealException(Throwable ex, Map<String, Object> map) {
        if (Objects.isNull(ex)) {
            return;
        }
        if (ex instanceof RpcInvokerException) {
            RpcInvokerException rpcInvokerException = (RpcInvokerException) ex;
            map.put("code", rpcInvokerException.getCode());
            map.put("message", rpcInvokerException.getMessage());
        } else if (ex instanceof RpcParseException) {
            RpcParseException rpcParseException = (RpcParseException) ex;
            map.put("code", rpcParseException.getCode());
            map.put("message", rpcParseException.getMessage());
        } else if (ex instanceof UnSupportSerialTypeException) {
            UnSupportSerialTypeException unSupportSerialTypeException = (UnSupportSerialTypeException) ex;
            map.put("code", 500);
            map.put("message", unSupportSerialTypeException.getMessage());
        } else {
            this.dealException(ex.getCause(), map);
        }
    }
}
