package com.hangu.rpc.consumer.handler;

import com.hangu.rpc.common.constant.hanguCons;
import com.hangu.rpc.common.entity.ParameterInfo;
import com.hangu.rpc.common.entity.PingPong;
import com.hangu.rpc.common.entity.Request;
import com.hangu.rpc.common.entity.Response;
import com.hangu.rpc.common.entity.RpcRequestTransport;
import com.hangu.rpc.common.entity.RpcResponseTransport;
import com.hangu.rpc.common.enums.ErrorCodeEnum;
import com.hangu.rpc.common.enums.MsgTypeMarkEnum;
import com.hangu.rpc.common.enums.SerializationTypeEnum;
import com.hangu.rpc.common.exception.RpcInvokerException;
import com.hangu.rpc.common.serialization.SerialInput;
import com.hangu.rpc.common.serialization.SerialOutput;
import com.hangu.rpc.common.util.CommonUtils;
import com.hangu.rpc.common.util.DescClassUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestMessageCodec extends MessageToMessageCodec<ByteBuf, Request> {

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (!ctx.channel().isWritable()) {
            ctx.channel().flush();
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Request request, List<Object> out) throws Exception {
        // 超过最高水位线，暂时不允许发送请求
        if (!ctx.channel().isWritable()) {
            throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "数据请求过于频繁！超过设置的最高水位线！");
        }
        ByteBuf byteBuf = ctx.alloc().buffer();
        // 魔数 2bytes
        byteBuf.writeShort(hanguCons.MAGIC);
        // 请求类型，序列化方式 1bytes
        byte finalMsgType =
            request.isHttp() ? MsgTypeMarkEnum.HTTP_REQUEST_FLAG.getMark() : MsgTypeMarkEnum.REQUEST_FLAG.getMark();
        byte serializationType = request.getSerializationType();
        finalMsgType |= serializationType;
        // 消息类型 1byte
        byteBuf.writeByte(finalMsgType);
        // 写入请求id
        byteBuf.writeLong(request.getId());
        // 写入内容
        RpcRequestTransport invokerContext = request.getInvokerTransport();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            SerialOutput serialOutput = SerializationTypeEnum.getOutputByType(outputStream, serializationType);
            String groupName = invokerContext.getGroupName();
            serialOutput.writeString(groupName);
            String interfaceName = invokerContext.getInterfaceName();
            serialOutput.writeString(interfaceName);
            String methodName = invokerContext.getMethodName();
            serialOutput.writeString(methodName);
            String version = invokerContext.getVersion();
            serialOutput.writeString(version);
            List<ParameterInfo> parameterInfos = Optional.ofNullable(invokerContext.getParameterInfos())
                .orElse(Collections.emptyList());
            String desc = parameterInfos.stream().map(e -> DescClassUtils.getDesc(e.getType()))
                .collect(Collectors.joining(","));
            serialOutput.writeString(desc);
            parameterInfos.stream().forEach(e -> {
                try {
                    serialOutput.writeObject(e.getValue());
                } catch (Throwable ex) {
                    throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "序列化失败！", ex);
                }
            });
            serialOutput.flush();
            byte[] contentBuff = outputStream.toByteArray();
            //内容对象长度 int 4bytes
            byteBuf.writeInt(contentBuff.length);
            //内容数据
            byteBuf.writeBytes(contentBuff);
        }
        out.add(byteBuf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("请求失败！", cause);
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        short magic = byteBuf.readShort();
        // 如果魔数不相等，那么认为这是一个无效的请求
        if (magic != hanguCons.MAGIC) {
            return;
        }
        byte msgType = byteBuf.readByte();
        // 判断请求类型
        int requstFlag = MsgTypeMarkEnum.REQUEST_FLAG.getMark() & msgType;
        // 请求id
        Long id = byteBuf.readLong();
        byte serialType = (byte) (hanguCons.SERIALIZATION_MARK & msgType);
        try {
            if ((MsgTypeMarkEnum.HEART_FLAG.getMark() & msgType) != 0) {

                PingPong pingPong = this.dealHeart(id, serialType);
                list.add(pingPong);
                // 响应
            } else if (requstFlag == 0) {
                Response response = this.dealResponse(id, byteBuf, serialType);
                list.add(response);
            }
        } catch (RpcInvokerException e) {
            Response response = CommonUtils.createResponseInfo(id, serialType, e.getCode(), e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        } catch (IOException e) {
            RpcInvokerException cause = new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "反序列化失败！", e);
            Response response = CommonUtils.createResponseInfo(id, serialType, cause.getCode(), cause.getClass(),
                cause);
            ctx.writeAndFlush(response);
            throw e;
        } catch (Exception e) {
            Response response = CommonUtils.createResponseInfo(id, serialType, ErrorCodeEnum.FAILURE.getCode(),
                e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        }
    }

    private Response dealResponse(Long id, ByteBuf byteBuf, byte serialType)
        throws IOException, ClassNotFoundException {

        int bodyLength = byteBuf.readInt();
        byte[] body = new byte[bodyLength];
        // 读取内容
        byteBuf.readBytes(body);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        SerialInput serialInput = SerializationTypeEnum.getInputByType(inputStream, serialType);
        // 响应编码
        int code = serialInput.readInt();
        String returnDesc = serialInput.readString();
        Class<?> clss = DescClassUtils.desc2class(returnDesc);
        Object value = serialInput.readObject(clss);
        RpcResponseTransport rpcResponseTransport = new RpcResponseTransport();
        rpcResponseTransport.setCode(code);
        rpcResponseTransport.setType(clss);
        rpcResponseTransport.setVale(value);
        Response response = new Response();
        response.setId(id);
        response.setSerializationType(serialType);
        response.setRpcResponseTransport(rpcResponseTransport);

        return response;
    }

    private PingPong dealHeart(Long id, byte serialType) {
        PingPong pingPong = new PingPong();
        pingPong.setId(id);
        pingPong.setSerializationType(serialType);
        return pingPong;
    }
}