package com.ytrue.gateway.center.domain.manage.model.vo;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 应用接口 VO
 */
public class ApplicationInterfaceVO {

    /**
     * 系统标识
     */
    private String systemId;
    /**
     * 接口标识
     */
    private String interfaceId;
    /**
     * 接口名称
     */
    private String interfaceName;
    /**
     * 接口版本
     */
    private String interfaceVersion;
    /**
     * 方法接口
     */
    private List<ApplicationInterfaceMethodVO> methodList;

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getInterfaceId() {
        return interfaceId;
    }

    public void setInterfaceId(String interfaceId) {
        this.interfaceId = interfaceId;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceVersion() {
        return interfaceVersion;
    }

    public void setInterfaceVersion(String interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    public List<ApplicationInterfaceMethodVO> getMethodList() {
        return methodList;
    }

    public void setMethodList(List<ApplicationInterfaceMethodVO> methodList) {
        this.methodList = methodList;
    }

}
