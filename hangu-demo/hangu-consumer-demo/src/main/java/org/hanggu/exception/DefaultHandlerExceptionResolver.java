package org.hangu.exception;

import cn.hutool.json.JSONUtil;
import com.hangu.common.exception.RpcInvokerException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
            if (ex instanceof RpcInvokerException) {
                RpcInvokerException rpcInvokerException = (RpcInvokerException) ex;
                map.put("code", rpcInvokerException.getCode());
            }
            map.put("message", ex.getMessage());
            response.getWriter().write(JSONUtil.toJsonStr(map));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ModelAndView();
    }
}
