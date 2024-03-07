package com.hangu.consumer.channel.handler;

import com.hangu.common.entity.PingPong;
import com.hangu.common.enums.SerializationTypeEnum;
import com.hangu.common.util.CommonUtils;
import com.hangu.consumer.client.ClientConnect;
import com.hangu.consumer.client.NettyClient;
import com.hangu.consumer.manager.ConnectManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 心跳处理器
 *
 * @author wuzhenhong
 * @date 2023/8/2 10:40
 */
@Slf4j
public class HeartBeatPongHandler extends ChannelInboundHandlerAdapter {

    private NettyClient nettyClient;

    private int retryBeat = 0;

    public HeartBeatPongHandler(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        // 尝试重连
        this.reconnect(ctx);
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 这里主要是为了解决网络抖动，误判机器下线，等网络正常时，注册中心再次通知
        // 那么需要重新标记为true
        this.nettyClient.getClientConnect().setRelease(false);
        this.nettyClient.getClientConnect().resetConnCount();
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 收到消息（无论是心跳消息还是任何其他rpc消息），重置重试发送心跳次数
        this.retryBeat = 0;
        // 这里主要是为了解决网络抖动，误判机器下线，等网络正常时，注册中心再次通知
        // 那么需要重新标记为true
        this.nettyClient.getClientConnect().setRelease(false);
        this.nettyClient.getClientConnect().resetConnCount();
        super.channelRead(ctx, msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState idleState = idleStateEvent.state();
            // 读超时，发送心跳
            if (IdleState.READER_IDLE == idleState) {
                if (retryBeat > 3) {
                    // 关闭重连，通过监听 channelInactive 发起重连
                    ctx.channel().close();
                } else {
                    PingPong pingPong = new PingPong();
                    pingPong.setId(CommonUtils.snowFlakeNextId());
                    pingPong.setSerializationType(SerializationTypeEnum.HESSIAN.getType());
                    // 发送心跳（从当前 context 往前）
                    ctx.writeAndFlush(pingPong).addListener(future -> {
                        if (!future.isSuccess()) {
                            log.error("发送心跳失败！", future.cause());
                        }
                    });
                    ++retryBeat;
                }
            } else {
                super.userEventTriggered(ctx, evt);
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private void reconnect(ChannelHandlerContext ctx) {

        ClientConnect clientConnect = this.nettyClient.getClientConnect();
        int retryConnectCount = clientConnect.incrConnCount();
        // N次之后还是不能连接上，放弃连接
        if (clientConnect.isRelease()) {
            return;
        }
        // 如果连接还活着，不需要重连
        if (clientConnect.isActive()) {
            return;
        }
        int delay = 2 * (retryConnectCount - 1);
        // 最大延迟20秒再执行
        if (delay > 20) {
            delay = 20;
        }

        ctx.channel().eventLoop().schedule(() -> {
            ConnectManager.doCacheReconnect(this.nettyClient);
        }, delay, TimeUnit.SECONDS);
    }
}
