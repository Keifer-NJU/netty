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

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
