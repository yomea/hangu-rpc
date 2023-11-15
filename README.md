# hangu-rpc

<div style="margin-top: 40px; margin-bottom: -30px;">
    <p align="center" style="display: flex; justify-content: center; gap: 20px;">
      <img src="https://img.shields.io/badge/hangu%20rpc-v0.5%20alpha-blue" style="max-width: 100px; height: auto;">
      <img src="https://img.shields.io/badge/Source-github-d021d6?style=flat&logo=GitHub" style="max-width: 100px; height: auto;">
      <img src="https://img.shields.io/badge/JDK-1.8+-ffcc00" style="max-width: 100px; height: auto;">
      <img src="https://img.shields.io/badge/Apache_License-2.0-33ccff" style="max-width: 100px; height: auto;">
    </p>
</div>

#### 一、介绍

该框架为rpc原理学习者提供一个思路，一个非常简单的轻量级rpc框架，
项目中没有使用非常复杂的设计，主打一个简单易用，快速上手，读懂源码实现，目前
注册中心默认实现了redis哨兵与zookeeper版本（个人建议使用zookeeper作为注册中心）。
如果你是一个对rpc原理好奇的人，你可以阅读本框架源码，快速了解rpc的核心实现。

hangu 是函谷的拼音。
为什么取这个名字呢？因为有一次和朋友聊天，聊到了《将夜》，说大师兄和余帘骑着一头牛进了一个叫函谷的地方，
于是道德经就出现了。所以我希望每个进入函谷的人，都能写出自己的道德经。

#### 二、软件架构

![image](https://github.com/yomea/hangu-rpc/assets/20855002/5aa5978f-5ab1-4dee-a3f8-74f00dbae2af)

从架构图中可以看到，心跳由消费者主动发起，默认每隔2s向服务提供者发送心跳包，心跳的实现很简单，在消费者这边
使用 Netty 提供的 IdleStateHandler 事件处理器，在每隔2s发起读超时事件时向提供者发送心跳，超过3次未收到
提供者的响应即认为需要重连，消费者端的 IdleStateHandler 配置代码如下：
```java
/**
 * 代码位置{@link com.hangu.consumer.client.NettyClient#start}
 */
// 省略前部分代码
.addLast(new ByteFrameDecoder())
.addLast(new RequestMessageCodec()) // 请求与响应编解码器
.addLast(new HeartBeatEncoder()) // 心跳编码器
.addLast("logging", loggingHandler)
// 每隔 2s 发送一次心跳，超过三次没有收到响应，也就是三倍的心跳时间，重连
.addLast(new IdleStateHandler(2, 0, 0, TimeUnit.SECONDS))
.addLast(new HeartBeatPongHandler(NettyClient.this)) // 心跳编码器
.addLast(new ResponseMessageHandler(executor));
// 省略后部分代码


```
可以看到有三个与心跳相关的处理器，分别为 HeartBeatEncoder，IdleStateHandler，HeartBeatPongHandler，
其中 IdleStateHandler 配置了读取超时为2s，超过2s没有收到读事件，那么就会发出读超时事件， 发出读超时事件之后，
由 HeartBeatPongHandler 处理该事件，处理的逻辑如下：
```java
@Override
protected void channelRead0(ChannelHandlerContext ctx, PingPong pingPong) throws Exception {
    // 收到消息，重置重试发送心跳次数
    this.retryBeat = 0;
}

@Override
public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
        IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
        IdleState idleState = idleStateEvent.state();
        // 读超时，发送心跳
        if (IdleState.READER_IDLE == idleState) {
            if (!ctx.channel().isActive()) {
                this.reconnect(ctx);
            } else if(retryBeat > 3) {
                // 重连
                this.reconnect(ctx);
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
        }
    }
}

private void reconnect(ChannelHandlerContext ctx) {
    ClientConnect clientConnect = ctx.channel()
        .attr(AttributeKey.<ClientConnect>valueOf(ctx.channel().id().asLongText())).get();
    ctx.channel().close().addListener(future -> {
        SocketAddress remoteAddress = ctx.channel().remoteAddress();
        if (!future.isSuccess()) {
            log.warn("通道{}关闭失败！", remoteAddress.toString());
            return;
        }
        ctx.channel().eventLoop().execute(() -> {
            // 重连创建一个新的通道
            nettyClient.reconnect(remoteAddress).addListener(f -> {
                if (!f.isSuccess()) {
                    log.error("重新连接{}失败！", remoteAddress.toString());
                } else {
                    if (Objects.nonNull(clientConnect)) {
                        ChannelFuture channelFuture = (ChannelFuture) f;
                        clientConnect.updateChannel(channelFuture.channel());
                    }
                }
            });
        });
    });
}
```

userEventTriggered 方法接收到读超时事件后，会判断当前连接是否已经有连续三次未接收到来自提供者的数据了，如果超过了，那么尝试重连，
如果没有超过三次，主动向提供者发出心跳，发出的心跳由 HeartBeatEncoder 编码器进行编码向提供者发送消息，在接收到提供者的响应之后，将重试次数归零。

这里我们使用 IdleStateHandler 的读超时实现了每隔2s向服务提供者发送心跳，超过三倍的心跳时间未接收到响应就认为该连接已断开，需要
重连。

服务提供者端不会主动向消费者发送心跳，它只会被动接收心跳，但超过8s未接收到任何来自消费者的读写数据时，主动关闭连接
```java
/**
 * 代码位置{@link com.hangu.provider.server.NettyServer#start}
 */
// 读写时间超过8s，表示该链接已失效
.addLast(new IdleStateHandler(0, 0, 8, TimeUnit.SECONDS))
```


#### 三、快速启动

##### 3.1 测试

hangu-demo里有两个子模块，分别是提供者和消费者，启动这两个模块，调用UserController的测试代码即可

##### 3.2 引入项目使用

###### 3.2.1 springboot项目

- 配置

如果你使用的时springboot，那么很好，直接引入以下依赖：

```xml
<dependency>
      <groupId>org.hangu</groupId>
      <artifactId>hangu-rpc-spring-boot-starter</artifactId>
      <version>1.0-SNAPSHOT</version>
    </dependency>
```
配置hangu-rpc(注册中心为redis哨兵模式的)：

```yaml
server:
  port: 8080
hangu:
  rpc:
    provider:
      port: 8089 # 提供者将要暴露的端口
    registry:
      protocol: redis # 选择zk作为注册中心
      redis:
        nodes: ip:port,ip2:port2 # redis哨兵模式哨兵集群地址
        master: xxx # master的名字
        password: xxx # redis的密码
```

配置hangu-rpc(注册中心为zk)：

```yaml
server:
  port: 8080
hangu:
  rpc:
    provider:
      port: 8089 # 提供者将要暴露的端口
    registry:
      protocol: zookeeper # 选择zk作为注册中心
      zookeeper:
        hosts: ip:port,ip2:port2 # zk集群地址
        user-name: yyy
        password: yyy
```

如果你有自己的注册中心，可以选择实现 com.hangu.common.registry.RegistryService 接口，然后将
hangu.rpc.registry.protocol=你自己的注册中心名字（其实这里本人使用的时springboot的@ConditionalOnProperty去动态加载， 也就是说你只要
保证spring容器中只存在一个实现了RegistryService接口的注册服务即可）

- 提供者

假设你有以下服务
```java
@HanguService
public class UserServiceImpl implements UserService {
    @Override
    public UserInfo getUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("小风");
        userInfo.setAge(27);
        Address address = new Address();
        address.setProvince("江西省");
        address.setCity("赣州市");
        address.setArea("于都县");
        userInfo.setAddress(address);
        return userInfo;
    }

    @Override
    public String getUserInfo(String name) {
        return name;
    }
}
```
接口为 UserService，实现类为 UserServiceImpl，如果你要暴露该接口，那么只需要在这个实现类上标注 @HanguService 注解即可，
这个注解目前有三个属性，分别为groupName，interfaceName，version，组通常是一系列相关业务的分组，通常会设置为服务名，因为我们的服务通常
会以业务进行垂直划分，interfaceName表示接口名，默认不填的情况下，会自动赋值为接口全路径类名，version表示版本，有时候我们给多个第三方提供服务的
时候可能业务上有差别，对于老的业务代码我们肯定不会去做改动，此时我们可以选择通过版本进行区分。

- 消费者

上面提供者提供了 UserService 服务，如果我们要引入这个服务怎么办？
```java
@HanguReference
public interface UserService {

    @HanguMethod(timeout = 20, callback = SimpleRpcResponseCallback.class)
    UserInfo getUserInfo(RpcResponseCallback callback);

    String getUserInfo(String name);
}
```
可以看到这个接口上标注了 @HanguReference 注解，这个注解与服务提供者的 @HanguService 注解一样拥有
groupName，interfaceName，version 三个属性，只要这三个属性与提供者的一一对应就能正常提供服务，这意味只要指定了
interfaceName这个名字与服务提供者一致，即使 UserService 这个接口类型的名字你瞎写，放在任何包下都可以正常服务，但是不建议
这么做，最好对接口进行抽离成单独的模块，打成jar包去引入

在类上指定了 @HanguReference 之后，我可以在方法上标注 @HanguMethod 注解，这个注解目前有 timeout与callback两个属性，
timeout用来指定该方法调用超时的时间，callback用来指定回调类实现，需要实现 RpcResponseCallback 接口，并且提供无参数构造器，
如果你不想在 @HanguMethod 注解上指定回调，那么可以在方法参数上指定实现了 RpcResponseCallback 接口的回调，这样方法由同步调用
变成了异步调用。

###### 3.2.2 普通spring项目

如果你是用的普通spring项目，想要引入hangu-rpc，那么你可以通过以下方式启动

```java

@EnableHanguRpc
public class HanguRpcBootstrapConfig {
    
}

```

注意：要确保加入了以下依赖(该依赖用于处理ConfigurationProperties注解)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
</dependency>
```

其他配置与消费者提供者的操作与springboot的一样

###### 3.2.2 普通项目

- 配置
如果你没有使用spring框架，那么你只需要在pom.xml中添加以下依赖
```xml
<dependency>
  <groupId>org.hangu</groupId>
  <artifactId>hangu-starter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```
- 提供者

依然假设你有一个叫 UserServiceImpl的接口实现

```java

public class Provider {
    public static void main(String[] args) {

        // 主要用于设置启动的端口
        HanguProperties hanguProperties = new HanguProperties();
        
        // 启动服务
        HanguRpcManager.openServer(hanguProperties);
        
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setGroupName("用户服务系统");
        serverInfo.setInterfaceName("userService");
        serverInfo.setVersion("1.0");
        Class<UserSerice> interfaceClass = UserService.class;
        UserService service = new UserServiceImpl();
        ZookeeperConfigProperties properties = new ZookeeperConfigProperties();
        properties.setHosts("ip:port,ip:port");
        RegistryService registryService = new ZookeeperRegistryService(properties);
        ServiceBean<T> serviceBean =
                new ServiceBean<>(serverInfo, interfaceClass, service, registryService);
        ServiceExporter.export(serviceBean);
    }
}


```

- 消费者

```java
public class Consumer {
    public static void main(String[] args) {

        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setGroupName("用户服务系统");
        serverInfo.setInterfaceName("userService");
        serverInfo.setVersion("1.0");

        Class<UserSerice> interfaceClass = UserService.class;
        ZookeeperConfigProperties properties = new ZookeeperConfigProperties();
        properties.setHosts("ip:port,ip:port");
        RegistryService registryService = new ZookeeperRegistryService(properties);
        
        // 主要用于设置消费者线程数量
        HanguProperties hanguProperties = new HanguProperties();

        ReferenceBean<T> referenceBean = new ReferenceBean<>(serverInfo, interfaceClass, registryService,
                hanguProperties);
        UserService service = ServiceReference.reference(referenceBean, CommonUtils.getClassLoader(this.getClass()));
        // 调用服务
        UserInfo userInfo = service.getUserInfo();
    }
}
```













