package com.ytrue.gateway.core.mapping;

/**
 * @author ytrue
 * @date 2023-09-06 17:06
 * @description HttpStatement
 */
public class HttpStatement {

    /**
     * 应用名称
     */
    private String application;
    /**
     * 服务接口；RPC、其他
     */
    private String interfaceName;
    /**
     * 服务方法；RPC#method
     */
    private String methodName;
    /**
     * 网关接口
     */
    private String uri;
    /**
     * 接口类型；GET、POST、PUT、DELETE
     */
    private HttpCommandType httpCommandType;


    /**
     * 参数类型(RPC 限定单参数注册)；new String[]{"java.lang.String"}、new String[]{"com.ytrue.gateway.rpc.dto.XReq"}
     */
    private String parameterType;

    /**
     * 是否鉴权；true = 是、false = 否
     */
    private boolean auth;


    public HttpStatement(String application, String interfaceName, String methodName, String parameterType, String uri, HttpCommandType httpCommandType, boolean auth) {
        this.application = application;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterType = parameterType;
        this.uri = uri;
        this.httpCommandType = httpCommandType;
        this.auth = auth;
    }



    //-------------------------------------get
    public String getApplication() {
        return application;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getUri() {
        return uri;
    }

    public HttpCommandType getHttpCommandType() {
        return httpCommandType;
    }

    public String getParameterType() {
        return parameterType;
    }

    public boolean isAuth() {
        return auth;
    }

}
