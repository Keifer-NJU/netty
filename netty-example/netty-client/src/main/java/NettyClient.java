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
//                         /*
//                         自定义序列化编解码器
//                         */
//                        // RpcResponse -> ByteBuf
//                        socketChannel.pipeline().addLast(new NettyKryoDecoder(kryoSerializer, RpcResponse.class));
//                        // ByteBuf -> RpcRequest
//                        socketChannel.pipeline().addLast(new NettyKryoEncoder(kryoSerializer, RpcRequest.class));
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
