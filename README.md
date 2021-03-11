



# Netty学习系列（一）-- 简单介绍及使用

### 1.为什么Netty

从官网上介绍，Netty是一个网络应用程序框架，开发服务器和客户端。也就是用于网络编程的一个框架。既然是网络编程，Socket就不谈了，为什么不用NIO呢？

#### 1.1 NIO的缺点

对于这个问题，之前我写了一篇文章[《NIO入门》](https://mp.weixin.qq.com/s/GfV9w2B0mbT7PmeBS45xLw)对NIO有比较详细的介绍，NIO的主要问题是：

- NIO的类库和API繁杂，学习成本高，你需要熟练掌握Selector、ServerSocketChannel、SocketChannel、ByteBuffer等。
- 需要熟悉Java多线程编程。这是因为NIO编程涉及到Reactor模式，你必须对多线程和网络编程非常熟悉，才能写出高质量的NIO程序。
- 臭名昭著的epoll bug。它会导致Selector空轮询，最终导致CPU 100%。直到JDK1.7版本依然没得到根本性的解决。

#### 1.2 Netty的优点

相对地，Netty的优点有很多：

- 统一简单的API，支持各种传输类型，阻塞与非阻塞的
- 功能强大，预置了多种编解码功能，支持多种主流协议
- 定制能力强，通过channelHandler对通信框架进行灵活扩展
- 自带编解码器解决TCP粘包/拆包问题
- 简单强大的线程模型
- 性能高
- 成熟，稳定，修复了大量的jdk nio bug
- 社区活跃
- 经历了大规模的商业应用考验，质量得到验证
- .......

### 2.Architectural Overview

![The Architecture Diagram of Netty](.\assert\architecture.png)

1. **丰富的缓冲架构**（ Rich Buffer Data Structure）

   Netty使用自己的缓冲区API代替NIO ByteBuffer来表示一个字节序列。这种方法比使用ByteBuffer有明显的优势。Netty的新型缓冲区类型，ChannelBuffer被从头设计来解决ByteBuffer的问题，并满足网络应用程序开发人员的日常需求。以下是一些很酷的功能:

   - 如果需要，您可以定义自己的缓冲区类型。
   - 透明零拷贝是通过内置的复合缓冲区类型实现的。
   - 动态缓冲区类型是开箱即用的，其容量随需扩展，就像StringBuffer一样。
   - 不再需要调用flip()。
   - 它通常比ByteBuffer更快。

2. **通用异步I/O API**

   Netty有一个称为Channel的通用异步I/O接口，它抽象了点对点通信所需的所有操作。也就是说，一旦您在一个Netty传输上编写了应用程序，您的应用程序就可以在其他Netty传输上运行。Netty通过一个通用API提供了许多基本的传输:

   - NIO-based TCP/IP transport (See `org.jboss.netty.channel.socket.nio`),
   - OIO-based TCP/IP transport (See `org.jboss.netty.channel.socket.oio`),
   - OIO-based UDP/IP transport, and
   - Local transport (See `org.jboss.netty.channel.local`).

3.  **基于拦截器链模式的事件模型(Event Model based on the Interceptor Chain Pattern)**

   ChannelEvent由ChannelPipeline中的ChannelHandlers列表处理。管道实现了拦截过滤器模式的高级形式，使用户可以完全控制如何处理事件以及管道中的处理程序如何相互交互。例如，你可以定义从套接字读取数据时应该做什么:

   ```java
     1 public class MyReadHandler implements SimpleChannelHandler {
     2     public void messageReceived(ChannelHandlerContext ctx, MessageEvent evt) {
               Object message = evt.getMessage();
     4         // Do something with the received message.
               ...
     6 
               // And forward the event to the next handler.
     8         ctx.sendUpstream(evt);
           }
    10 }
   ```

   You can also define what to do when a handler receives a write request:

   ```java
     1 public class MyWriteHandler implements SimpleChannelHandler {
     2     public void writeRequested(ChannelHandlerContext ctx, MessageEvent evt) {
               Object message = evt.getMessage();
     4         // Do something with the message to be written.
               ...
     6 
               // And forward the event to the next handler.
     8         ctx.sendDownstream(evt);
           }
    10 }
   ```

```
                                                      |
  +---------------------------------------------------+---------------+
  |                           ChannelPipeline         |               |
  |                                                  \|/              |
  |    +---------------------+            +-----------+----------+    |
  |    | Inbound Handler  N  |            | Outbound Handler  1  |    |
  |    +----------+----------+            +-----------+----------+    |
  |              /|\                                  |               |
  |               |                                  \|/              |
  |    +----------+----------+            +-----------+----------+    |
  |    | Inbound Handler N-1 |            | Outbound Handler  2  |    |
  |    +----------+----------+            +-----------+----------+    |
  |              /|\                                  .               |
  |               .                                   .               |
  | ChannelHandlerContext.fireIN_EVT() ChannelHandlerContext.OUT_EVT()|
  |        [ method call]                       [method call]         |
  |               .                                   .               |
  |               .                                  \|/              |
  |    +----------+----------+            +-----------+----------+    |
  |    | Inbound Handler  2  |            | Outbound Handler M-1 |    |
  |    +----------+----------+            +-----------+----------+    |
  |              /|\                                  |               |
  |               |                                  \|/              |
  |    +----------+----------+            +-----------+----------+    |
  |    | Inbound Handler  1  |            | Outbound Handler  M  |    |
  |    +----------+----------+            +-----------+----------+    |
  |              /|\                                  |               |
  +---------------+-----------------------------------+---------------+
                  |                                  \|/
  +---------------+-----------------------------------+---------------+
  |               |                                   |               |
  |       [ Socket.read() ]                    [ Socket.write() ]     |
  |                                                                   |
  |  Netty Internal I/O Threads (Transport Implementation)            |
  +-------------------------------------------------------------------+
 
```

在三个核心之上，Netty已经实现了所有类型的网络应用程序，它提供了一组高级特性来进一步加速开发页面。

### 3.核心类

- #### EventLoop  & EventLoopGroup

  EventLoopGroup可以理解为线程池，EventLoop理解为一个线程，每个EventLoop对应一个Selector，负责处理多个Channel上的事件。

  这是Netty处理事件的核心机制。上面我使用了**EventLoopGroup**。我们在NIO中通常要做的几件事情，如注册感兴趣的事件、调度相应的Handler等，都是**EventLoop**负责。Channel、EventLoop、Thread以及EventLoopGroup 之间的关系可以用如下图表示：

  ![img](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\1)

  一个 EventLoopGroup 包含一个或多个 EventLoop ，即 EventLoopGroup : EventLoop = 1 : n 。

  一个 EventLoop 在它的生命周期内，只能与一个 Thread 绑定，即 EventLoop : Thread = 1 : 1 。

  所有有 EventLoop 处理的 I/O 事件都将在它**专有**的 Thread 上被处理，从而保证线程安全，即 Thread : EventLoop = 1 : 1。

  一个 Channel 在它的生命周期内只能注册到一个 EventLoop 上，即 Channel : EventLoop = n : 1 。

  一个 EventLoop 可被分配至一个或多个 Channel ，即 EventLoop : Channel = 1 : n 。

  当一个连接到达时，Netty 就会创建一个 Channel，然后从 EventLoopGroup 中分配一个 EventLoop 来给这个 Channel 绑定上，在该 Channel 的整个生命周期中都是有这个绑定的 EventLoop 来服务的。

- #### NioEventLoopGroup

  是一个处理I/O操作的多线程事件循环。Netty为不同类型的传输提供了各种EventLoopGroup实现。在本例中，我们将实现一个服务器端应用程序，因此将使用两个NioEventLoopGroup。第一个通常被称为“boss”，它接受一个传入的连接。第二个，通常称为“worker”，在“boss"接受连接并将接受的连接注册到worker之后，处理已接受连接的流量。使用多少线程以及如何将它们映射到创建的通道取决于EventLoopGroup实现，甚至可以通过构造函数进行配置。

#### ServerBootstrap

是一个设置服务器的助手类。您可以直接使用 [`Channel`](https://netty.io/4.1/api/io/netty/channel/Channel.html) 设置服务器。但是，请注意，这是一个冗长的过程，在大多数情况下不需要这样做。

**ServerBootstrap**，**服务器端**程序的**入口**，这是Netty为简化网络程序配置和关闭等生命周期管理，所引入的Bootstrapping机制。我们通常要做的创建Channel、绑定端口、注册Handler等，都可以通过这个统一的入口，以**Fluent API**等形式完成，相对**简化了API使用**。与之相对应， **Bootstrap**则是**Client端**的通常入口。

![image.png](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\ae5c6ed3008d4323aaa817e9cb46437a.png)

#### Bootstrap

与ServerBootstrap类似，不同之处是它适用于非服务器通道，如客户端或无连接通道

#### Selector

Netty基于java.nio.channels.Selector对象实现IO多路复用，通过Selector一个线程可以监听多个连接的Channel事件。当向一个Selector中注册Channel后，Selector内部的机制就可以自动不断的Select这些注册的Channel是否有就绪的IO事件（可读、可写、网络连接完成等）。

#### Channel

**channel**它代表一个到**实体**（如一个硬件设备、一个文件、一个网络套接字或者一个能够执行一个或者多个不同的I/O操作的程序组件）的开放连接，如读操作和写操作。目前，可以把**Channel** 看作是传入（入站）或者传出（出站）数据的载体。因此，它可以被打开或者被关闭，连接或者断开连接。其实Channel就对应着传统网络编程中的**Socket连接**。

Channel提供异步的网络IO操作，调用后立即返回ChannelFuture，通过注册监听，或者同步等待，最终获取结果。

Channel根据不同的协议、不同的阻塞类型，分为不同的Channel类型：

![channel.png](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\45aedb41ab7f44c60a901d5a6a2a2472.jpg)

#### ChannelPipeline 、 ChannelHandler、ChannelHandlerContext

**ChannelHandler:** 这是应用开发者放置业务逻辑的主要地方，**Handler**主要的操作为**Channel缓存读**、**数据解码**、**业务处理**、**写Channel缓存**，然后由Channel（代表client）发送到最终的连接终端。

**ChannelPipeline:** 它是ChannelHandler**链条的容器**，每个Channel在创建后，自动被分配一个**ChannelPipeline**。在上面的示例中，我们通过**ServerBootstrap**注册了ChannelInitializer，并且实现了initChannel方法，而在该方法中则承担了向ChannelPipleline安装其他Handler的任务。它在处理ChannelHandler时采用链式调用

**每个Channel都有且只有一个ChannelPipeline与之对应。**

Netty 的 Channel 过滤器实现原理与 Servlet Filter 机制一致，它将 Channel 的数据管道抽象为 ChannelPipeline，消息在 ChannelPipeline 中流动和传递。ChannelPipeline 持有 I/O 事件拦截器 ChannelHandler 的链表，由 ChannelHandler 对 I/O 事件进行拦截和处理，可以方便地通过新增和删除 ChannelHandler 来实现不同的业务逻辑定制，不需要对已有的 ChannelHandler 进行修改，能够实现对修改封闭和对扩展的支持。ChannelPipeline 是 ChannelHandler 的容器，它负责 ChannelHandler 的管理和事件拦截与调度

**ChannelHandlerContext：**保存Channel相关的所有上下文信息，同时关联一个ChannelHandler。允许ChannelHandler与它的ChannelPipeline和其他Handler交互。处理程序可以通知ChannelPipeline中的下一个ChannelHandler，也可以动态地修改它所属的ChannelPipeline。 



**一个 Channel 包含了一个 ChannelPipeline，而 ChannelPipeline 中又维护了一个由 ChannelHandlerContext 组成的双向链表，并且每个 ChannelHandlerContext 中又关联着一个 ChannelHandler。**

![channelpipeline.jpg](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\060b6337573afd1d8ba11b8c73b0712b.jpg)

#### ChannelInitializer

这里指定的处理程序将始终由新接受的Channel进行计算。ChannelInitializer是一个特殊的处理程序，用于帮助用户配置新Channel。您很可能希望通过添加一些处理程序(如DiscardServerHandler)来配置新通道的ChannelPipeline来实现您的网络应用程序。随着应用程序变得复杂，很可能会向管道添加更多的处理程序，并最终将这个匿名类提取到顶级类中。

#### [ChannelFuture](https://netty.io/4.1/api/index.html)

Netty中的**所有I/O操作**都是异步的。这意味着任何I/O调用都会立即返回，但不能保证请求的I/O操作在调用结束时已经完成。相反，您将返回一个ChannelFuture实例，它为您提供有关I/O操作的结果或状态的信息。

ChannelFuture表示一个尚未发生的I/O操作。这意味着，任何请求的操作可能还没有被执行，因为在Netty中所有操作都是异步的。例如，以下代码可能会在消息发送之前关闭连接:

```java
Channel ch = ...;
ch.writeAndFlush(message);
ch.close();
```

因此，您需要在ChannelFuture完成后调用close()方法(该方法由write()方法返回)，并在写操作完成时通知它的侦听器。请注意，close()也可能不会立即关闭连接，它会返回一个ChannelFuture

```java
final ChannelFuture f = ctx.writeAndFlush(time); // (3)
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                assert f == future;
                ctx.close();
            }
        }); // (4)
```

#### Future 

在 Netty 中，所有的 I/O 操作都是异步的，这意味着任何 I/O 调用都会立即返回，而不是像传统 BIO 那样同步等待操作完成。异步操作会带来一个问题：调用者如何获取异步操作的结果？ChannelFuture 就是为了解决这个问题而专门设计的。下面我们一起看它的原理。ChannelFuture 有两种状态：uncompleted 和 completed。当开始一个 I/O 操作时，一个新的 ChannelFuture 被创建，此时它处于 uncompleted 状态——非失败、非成功、非取消，因为 I/O 操作此时还没有完成。一旦 I/O 操作完成，ChannelFuture 将会被设置成 completed



备注：由于Netty中的Future都是异步IO操作，结果是未知的，因此命名为ChannelFuture，表示跟Channel的操作有关。Netty强烈建议直接通过添加监听器的方式获取IO结果，而不是通过同步等待的方式。如果用户操作调用了sync或者await方法，会在对应的future对象上阻塞用户线程，例如future.channel().closeFuture().sync()



**几个组件的关系图：**

![img](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\2)

### 4.线程处理模型

- **https://juejin.cn/post/6856765203313508365#heading-6**
  - 串行处理模型
  - 并行处理模型
  - Netty线程处理模型
  - ![img](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\157ccb40c65f4c118876221d7c61e7ac~tplv-k3u1fbpfcp-watermark.image)
  - 这上面有几个注意的地方，这里我简单的进行分析。首先是关于NioEventLoop和NioEventLoopGroup的。NioEventLoop实际上就是工作线程，可以直接理解为一个线程。NioEventLoopGroup是一个线程池，线程池中的线程就是NioEventLoop。实际上bossGroup中有多个NioEventLoop线程，每个NioEventLoop绑定一个端口，也就是说，如果程序只需要监听1个端口的话，bossGroup里面只需要有一个NioEventLoop线程就行了。     每个NioEventLoop都绑定了一个Selector，所以在Netty的线程模型中，是由多个Selecotr在监听IO就绪事件。而Channel注册到Selector。     一个Channel绑定一个NioEventLoop，相当于一个连接绑定一个线程，这个连接所有的ChannelHandler都是在一个线程中执行的，避免了多线程干扰。更重要的是ChannelPipline链表必须严格按照顺序执行的。单线程的设计能够保证ChannelHandler的顺序执行。     一个NioEventLoop的selector可以被多个Channel注册，也就是说多个Channel共享一个EventLoop。EventLoop的Selecctor对这些Channel进行检查。
- **[NIO之Reactor模式,Netty序章](https://segmentfault.com/a/1190000019469833)  **（帮助理解Reactor线程模型）

### 5.关键流程学习

**Netty服务端创建流程**

![The Architecture Diagram of Netty](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\lmt8euzs30.jpeg)



**Netty客户端创建流程**

![The Architecture Diagram of Netty](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\0lu01vdxa9.jpeg)

### 6.入门使用例子

![image.png](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\cc27d56addd74e82b6b6b349c7f3769b.png)

#### 6.1传输实体类

**客户端**

```java
package dto;

/**
 * @author Keifer
 * @createTime 2021/3/10 21:54
 */
public class RpcRequest {
    private String message;
    private String name;

    public RpcRequest(String message, String name) {
        this.message = message;
        this.name = name;
    }
    public String getMessage() {
        return message;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "message='" + message + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
```

**服务端**

```java
package dto;

/**
 * @author Keifer
 * @createTime 2021/3/10 21:56
 */
public class RpcResponse {
    private String message;
    private String code;

    @Override
    public String toString() {
        return "RpcResponse{" +
                "message='" + message + '\'' +
                ", code=" + code +
                '}';
    }

    public RpcResponse(String message, String code) {
        this.message = message;
        this.code = code;
    }
    public String getMessage() {
        return message;
    }
    public String getCode() {
        return code;
    }

}
```

#### 6.2客户端

- **初始化客户端**

  客户端通过SendMessage负责向服务端发送消息**（本来是RpcRequest实体类，但是需要涉及编码与解码，所以这一版本用ByteBuf代替做示例，看代码时不要懵）**,并能获取服务端返回的结果。

```java
import dto.RpcRequest;
import dto.RpcResponse;
import encode.NettyKryoDecoder;
import encode.NettyKryoEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serializer.KryoSerializer;

/**
 * @author Keifer
 * @createTime 2021/3/10 21:30
 */
public class NettyClient {
    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);
    private final String host;
    private final int port;
    private static final Bootstrap b;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    static {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        b = new Bootstrap();
        KryoSerializer kryoSerializer = new KryoSerializer();
        b.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new MyNettyClientHandler());
                    }
                });

    }
    public RpcResponse sendMessage(final ByteBuf rpcrequest){
        try{
            ChannelFuture channelFuture = b.connect(host, port).sync();
            logger.info("client connect:{}",host+":"+port);
            Channel channel = channelFuture.channel();
            logger.info("send message");
            if(channel!=null){
                channel.writeAndFlush(rpcrequest).addListener(future->{
                    if(future.isSuccess()){
                        logger.info("client send message :[{}]", rpcrequest.toString());
                    }else{
                        logger.error("Send message failed:",future.cause());
                    }
                });
                channel.closeFuture().sync();
                return null;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
```

- **自定义ChannelHandler 处理服务器返回的消息**

简单的在日志中打印出信息

```java
import dto.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author Keifer
 * @createTime 2021/3/10 21:49
 */
public class MyNettyClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MyNettyClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try{
            ByteBuf rpcResponse = (ByteBuf) msg;
            long currentTimeMills = (rpcResponse.readUnsignedInt()-2208988800L)*1000L;
            LOGGER.info("client receive msg:{}", new Date(currentTimeMills));
            ctx.close();
        }finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("client caught exception", cause);
        ctx.close();
    }
}
```

#### 6.3服务端

- **初始化服务端**

服务端开启一个服务用于接收客户端的请求，处理并返回结果

```java
import dto.RpcRequest;
import dto.RpcResponse;
import encode.NettyKryoDecoder;
import encode.NettyKryoEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import serializer.KryoSerializer;

/**
 * @author Keifer
 * @createTime 2021/3/10 22:08
 */
public class NettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
    private final int port;

    public NettyServer(int port) {
        this.port = port;
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        KryoSerializer kryoSerializer = new KryoSerializer();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new NettyServerHandler());
                        }
                    });

            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();
            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.error("occur exception when start server:", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
```

- **自定义ChannelHandler处理客户端消息**

  **同样返回消息也是ByteBuf，存储的当前时间，实际应用中需要结合编码解码等，传输实体类**

  ```java
  import com.sun.org.apache.bcel.internal.generic.LOOKUPSWITCH;
  import dto.RpcRequest;
  import dto.RpcResponse;
  import io.netty.buffer.ByteBuf;
  import io.netty.channel.ChannelFuture;
  import io.netty.channel.ChannelFutureListener;
  import io.netty.channel.ChannelHandlerContext;
  import io.netty.channel.ChannelInboundHandlerAdapter;
  import io.netty.util.ReferenceCountUtil;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  
  import java.util.Date;
  
  /**
   * @author Keifer
   * @createTime 2021/3/10 22:11
   */
  public class NettyServerHandler extends ChannelInboundHandlerAdapter {
      private static final Logger LOGGER = LoggerFactory.getLogger(NettyServerHandler.class);
      @Override
      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
          try {
              ByteBuf rpcRequest = (ByteBuf) msg;
              long currentTimeMills = (rpcRequest.readUnsignedInt()-2208988800L)*1000L;
              LOGGER.info("server receive message:{}",new Date(currentTimeMills));
  //            RpcResponse rpcResponse = new RpcResponse("message from server","200");
              final ByteBuf time = ctx.alloc().buffer(4); // (2)
              time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));
              ChannelFuture channelFuture = ctx.writeAndFlush(time);
              channelFuture.addListener(ChannelFutureListener.CLOSE);
          }finally {
              ReferenceCountUtil.release(msg);
          }
      }
  
      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
          LOGGER.error("server caught exception", cause);
          ctx.close();
      }
  }
  ```

#### 6.4测试

- **客户端 ** ClientMain

```java
import dto.RpcRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Keifer
 * @createTime 2021/3/10 22:18
 */
public class ClientMain {
    public static void main(String[] args){
        RpcRequest rpcRequest = new RpcRequest("hello ae86", "18 age");
        NettyClient nettyClient = new NettyClient("localhost",8080);
        final ByteBuf time = Unpooled.buffer(4);
        time.writeInt((int) (System.currentTimeMillis() / 1000L + 2208988800L));
        nettyClient.sendMessage(time);
    }
}
```

- **服务端 ** ServerMain

```java
/**
 * @author Keifer
 * @createTime 2021/3/10 22:17
 */
public class ServerMain {
    public static void main(String[] args){
        NettyServer nettyServer = new NettyServer(8080);
        nettyServer.run();
    }
}
```

![image-20210311103241886](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\image-20210311103241886.png)

![image-20210311103312548](D:\OneDriver_edu\OneDrive - smail.nju.edu.cn\Java\MarkDown_AE86\Netty\assert\image-20210311103312548.png)

本节 Over!

**[完整源码地址]()**

