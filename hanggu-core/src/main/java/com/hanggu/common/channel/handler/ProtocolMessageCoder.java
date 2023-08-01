package com.hanggu.common.channel.handler;

import com.hanggu.common.constant.HangguCons;
import com.hanggu.common.entity.ParameterInfo;
import com.hanggu.common.entity.PingPong;
import com.hanggu.common.entity.Request;
import com.hanggu.common.entity.Response;
import com.hanggu.common.entity.RpcRequestTransport;
import com.hanggu.common.entity.RpcResponseTransport;
import com.hanggu.common.enums.MsgTypeMarkEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.serialization.SerialInput;
import com.hanggu.common.serialization.SerialOutput;
import com.hanggu.common.util.DescClassUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolMessageCoder extends MessageToMessageCodec<ByteBuf, Request> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Request request, List<Object> out) throws Exception {
        ByteBuf byteBuf = ctx.alloc().buffer();
        // 魔数 2bytes
        byteBuf.writeShort(HangguCons.MAGIC);
        // 请求类型，序列化方式 1bytes
        byte finalMsgType = MsgTypeMarkEnum.REQUEST_FLAG.getMark();
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
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            byte[] contentBuff = outputStream.toByteArray();
            //内容对象长度 int 4bytes
            byteBuf.writeInt(contentBuff.length);
            //内容数据
            byteBuf.writeBytes(contentBuff);
        }
        out.add(byteBuf);
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        short magic = byteBuf.readShort();
        // 如果魔数不相等，那么认为这是一个无效的请求
        if(magic != HangguCons.MAGIC) {
            return;
        }
        byte msgType = byteBuf.readByte();
        // 判断请求类型
        int requstFlag = MsgTypeMarkEnum.REQUEST_FLAG.getMark() & msgType;
        // 表示是来自客户端的请求
        if(requstFlag == 1) {
            int bodyLength = byteBuf.readInt();
            byte[] body = new byte[bodyLength];
            // 读取内容
            byteBuf.readBytes(body);
            byte serialType = (byte) (HangguCons.SERIALIZATION_MARK & msgType);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
            SerialInput serialInput = SerializationTypeEnum.getInputByType(inputStream, serialType);
            String groupName = serialInput.readString();
            String interfaceName = serialInput.readString();
            String methodName = serialInput.readString();
            String version = serialInput.readString();
            String parameters = serialInput.readString();
            List<String> parameterTypeList = Arrays.stream(parameters.split(",")).collect(Collectors.toList());
            List<ParameterInfo> parameterInfos = parameterTypeList.stream().map(desc -> {
                try {
                    Class<?> clss = DescClassUtils.desc2class(desc);
                    Object value = serialInput.readObject(clss);
                    ParameterInfo parameterInfo = new ParameterInfo();
                    parameterInfo.setType(clss);
                    parameterInfo.setValue(value);
                    return parameterInfo;
                } catch (ClassNotFoundException e) {
                    // TODO: 2023/8/1 构建错误响应给客户端
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    // TODO: 2023/8/1 构建错误响应给客户端
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            RpcRequestTransport rpcRequestTransport = new RpcRequestTransport();
            rpcRequestTransport.setGroupName(groupName);
            rpcRequestTransport.setInterfaceName(interfaceName);
            rpcRequestTransport.setMethodName(methodName);
            rpcRequestTransport.setVersion(version);
            rpcRequestTransport.setParameterInfos(parameterInfos);
            // 请求id
            Long id = byteBuf.readLong();
            Request request = new Request();
            request.setId(id);
            request.setSerializationType(serialType);
            request.setInvokerTransport(rpcRequestTransport);
            list.add(request);
            // 心跳
        } else if((MsgTypeMarkEnum.HEART_FLAG.getMark() & msgType) == 1) {

            Long id = byteBuf.readLong();
            PingPong pingPong = new PingPong();
            pingPong.setId(id);
            list.add(pingPong);
            // 响应
        } else {
            // 请求id
            Long id = byteBuf.readLong();
            int bodyLength = byteBuf.readInt();
            byte[] body = new byte[bodyLength];
            // 读取内容
            byteBuf.readBytes(body);
            byte serialType = (byte) (HangguCons.SERIALIZATION_MARK & msgType);
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
            list.add(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("请求编码失败！", cause);
        super.exceptionCaught(ctx, cause);
    }
}