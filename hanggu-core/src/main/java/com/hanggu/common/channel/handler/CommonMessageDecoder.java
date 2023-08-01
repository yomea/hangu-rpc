package com.hanggu.common.channel.handler;

import com.hanggu.common.constant.HangguCons;
import com.hanggu.common.entity.*;
import com.hanggu.common.enums.ErrorCodeEnum;
import com.hanggu.common.enums.MsgTypeMarkEnum;
import com.hanggu.common.enums.SerializationTypeEnum;
import com.hanggu.common.exception.RpcInvokerException;
import com.hanggu.common.serialization.SerialInput;
import com.hanggu.common.util.CommonUtils;
import com.hanggu.common.util.DescClassUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by wuzhenhong on 2023/8/2 00:48
 */
@Slf4j
public class CommonMessageDecoder extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        short magic = byteBuf.readShort();
        // 如果魔数不相等，那么认为这是一个无效的请求
        if (magic != HangguCons.MAGIC) {
            return;
        }
        byte msgType = byteBuf.readByte();
        // 判断请求类型
        int requstFlag = MsgTypeMarkEnum.REQUEST_FLAG.getMark() & msgType;
        // 请求id
        Long id = byteBuf.readLong();
        byte serialType = (byte) (HangguCons.SERIALIZATION_MARK & msgType);
        try {
            // 表示是来自客户端的请求
            if (requstFlag == 1) {
                Request request = this.dealRequest(id, byteBuf, serialType);
                list.add(request);
                // 心跳
            } else if ((MsgTypeMarkEnum.HEART_FLAG.getMark() & msgType) == 1) {

                PingPong pingPong = this.dealHeart(id);
                list.add(pingPong);
                // 响应
            } else {
                Response response = this.dealResponse(id, byteBuf, serialType);
                list.add(response);
            }
        } catch (RpcInvokerException e) {
            Response response = CommonUtils.createResponseInfo(id, serialType, e.getCode(), e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        } catch (IOException e) {
            RpcInvokerException cause = new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "反序列化失败！", e);
            Response response = CommonUtils.createResponseInfo(id, serialType, cause.getCode(), cause.getClass(), cause);
            ctx.writeAndFlush(response);
            throw e;
        } catch (Exception e) {
            Response response = CommonUtils.createResponseInfo(id, serialType, ErrorCodeEnum.FAILURE.getCode(), e.getClass(), e);
            ctx.writeAndFlush(response);
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("请求编码失败！", cause);
        super.exceptionCaught(ctx, cause);
    }

    private Request dealRequest(Long id, ByteBuf byteBuf, byte serialType) throws IOException {

        int bodyLength = byteBuf.readInt();
        byte[] body = new byte[bodyLength];
        // 读取内容
        byteBuf.readBytes(body);
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
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), String.format("调用方法参数类型描述=》%s，类不存在！", desc), e);
            } catch (IOException e) {
                throw new RpcInvokerException(ErrorCodeEnum.FAILURE.getCode(), "反序列化异常", e);
            }
        }).collect(Collectors.toList());
        RpcRequestTransport rpcRequestTransport = new RpcRequestTransport();
        rpcRequestTransport.setGroupName(groupName);
        rpcRequestTransport.setInterfaceName(interfaceName);
        rpcRequestTransport.setMethodName(methodName);
        rpcRequestTransport.setVersion(version);
        rpcRequestTransport.setParameterInfos(parameterInfos);
        Request request = new Request();
        request.setId(id);
        request.setSerializationType(serialType);
        request.setInvokerTransport(rpcRequestTransport);


        return request;
    }

    private Response dealResponse(Long id, ByteBuf byteBuf, byte serialType) throws IOException, ClassNotFoundException {

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

    private PingPong dealHeart(Long id) {
        PingPong pingPong = new PingPong();
        pingPong.setId(id);
        return pingPong;
    }
}
