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
