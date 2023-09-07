package com.ytrue.gateway.core.executor.result;

/**
 * @author ytrue
 * @date 2023-09-07 10:49
 * @description GatewayResult
 */
public class SessionResult {

    private String code;
    private String info;
    private Object data;


    protected SessionResult(String code, String info, Object data) {
        this.code = code;
        this.info = info;
        this.data = data;
    }

    public static SessionResult buildSuccess(Object data) {
        return new SessionResult("0000", "调用成功", data);
    }

    public static SessionResult buildError(Object data) {
        return new SessionResult("0001", "调用失败", data);
    }


    // --------------------------get
    public String getCode() {
        return code;
    }

    public String getInfo() {
        return info;
    }

    public Object getData() {
        return data;
    }
}
