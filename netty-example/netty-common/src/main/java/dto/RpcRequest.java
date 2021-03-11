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
