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
