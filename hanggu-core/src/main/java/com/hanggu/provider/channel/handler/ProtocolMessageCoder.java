package com.hanggu.provider.channel.handler;

import com.hanggu.common.entity.HangguProtocol;
import com.hanggu.common.enums.MsgTypeMarkEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.serialization.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolMessageCoder extends MessageToMessageCodec<ByteBuf, HangguProtocol> {

    @Override
    protected void encode(ChannelHandlerContext ctx, HangguProtocol msg, List<Object> out) throws Exception {
        ByteBuf byteBuf = ctx.alloc().buffer();
        //魔数 2bytes
        byteBuf.writeBytes(msg.getMagic());
        // 请求类型，序列化方式 1bytes
        Integer msgType = msg.getMsgType();
        byte finalMsgType = MsgTypeMarkEnum.getMarkByType(msgType);
        byte serializationType = msg.getSerializationType();
        finalMsgType |= serializationType;
        //消息类型 1byte
        byteBuf.writeByte(finalMsgType);
        Object body = msg.getMsg();
        Serialization serialization = SerializationTypeEnum.getSerializationByType(serializationType);
        byte[] contentBuff = serialization.serialize(body);
        //内容对象长度 int 4bytes
        byteBuf.writeInt(contentBuff.length);
        //内容数据
        byteBuf.writeBytes(contentBuff);
        out.add(byteBuf);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {

    }

}