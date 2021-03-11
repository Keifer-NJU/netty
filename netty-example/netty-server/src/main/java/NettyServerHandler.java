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
