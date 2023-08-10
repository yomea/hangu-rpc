package com.hanggu.common.channel.handler;

import com.hanggu.common.constant.HangguCons;
import com.hanggu.common.entity.PingPong;
import com.hanggu.common.enums.MsgTypeMarkEnum;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳编码器
 *
 * @author wuzhenhong
 * @date 2023/8/2 9:44
 */
@Slf4j
public class HeartBeatEncoder extends ChannelDuplexHandler {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof PingPong)) {
            return;
        }

        PingPong pingPong = (PingPong) msg;

        ByteBuf byteBuf = ctx.alloc().buffer();
        // 魔数 2bytes
        byteBuf.writeShort(HangguCons.MAGIC);
        // 请求类型，序列化方式 1bytes
        byte finalMsgType = MsgTypeMarkEnum.HEART_FLAG.getMark();
        byte serializationType = pingPong.getSerializationType();
        finalMsgType |= serializationType;
        // 消息类型 1byte
        byteBuf.writeByte(finalMsgType);
        // 写入请求id
        byteBuf.writeLong(pingPong.getId());
        byteBuf.writeInt(0);

        ctx.writeAndFlush(byteBuf).addListener(future -> {
            if(!future.isSuccess()) {
                log.error("发送心跳失败！。。。。。。。。。。。。。。。。。。。", future.getNow());
            }
        });
    }
}
