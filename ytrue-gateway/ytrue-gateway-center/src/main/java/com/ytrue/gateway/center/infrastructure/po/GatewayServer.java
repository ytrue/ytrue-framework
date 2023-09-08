package com.ytrue.gateway.center.infrastructure.po;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 网关服务
 */
public class GatewayServer {

    /**
     * 自增主键
     */
    private Integer id;
    /**
     * 分组标识
     */
    private String groupId;
    /**
     * 分组名称
     */
    private String groupName;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

}
