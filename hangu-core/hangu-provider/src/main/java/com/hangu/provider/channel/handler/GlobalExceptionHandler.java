package com.hangu.provider.channel.handler;

import com.hangu.common.constant.hanguCons;
import com.hangu.common.entity.Request;
import com.hangu.common.entity.Response;
import com.hangu.common.enums.ErrorCodeEnum;
import com.hangu.common.util.CommonUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * @author wuzhenhong
 * @date 2024/2/20 15:56
 */
@Slf4j
@Sharable
public class GlobalExceptionHandler extends ChannelDuplexHandler  {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("请求处理失败！", cause);
        Request request = ctx.channel().attr(hanguCons.REQUEST_ATTRIBUTE_KEY).get();
        if(Objects.nonNull(request)) {
            // 如果是其他在处理请求过程中抛出的异常，那么需要通知到消费者
            Response response = CommonUtils.createResponseInfo(request.getId(), request.getSerializationType(),
                ErrorCodeEnum.FAILURE.getCode(), cause.getClass(), cause);
            ctx.channel().writeAndFlush(response);
        }
        super.exceptionCaught(ctx, cause);
    }
}
