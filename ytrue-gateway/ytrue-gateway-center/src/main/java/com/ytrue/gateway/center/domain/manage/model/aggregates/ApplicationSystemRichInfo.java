package com.ytrue.gateway.center.domain.manage.model.aggregates;



import com.ytrue.gateway.center.domain.manage.model.vo.ApplicationSystemVO;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 网关算力配置信息
 */
public class ApplicationSystemRichInfo {

    /**
     * 网关ID
     */
    private String gatewayId;
    /**
     * 系统列表
     */
    private List<ApplicationSystemVO> applicationSystemVOList;

    public ApplicationSystemRichInfo() {
    }

    public ApplicationSystemRichInfo(String gatewayId, List<ApplicationSystemVO> applicationSystemVOList) {
        this.gatewayId = gatewayId;
        this.applicationSystemVOList = applicationSystemVOList;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public List<ApplicationSystemVO> getApplicationSystemVOList() {
        return applicationSystemVOList;
    }

    public void setApplicationSystemVOList(List<ApplicationSystemVO> applicationSystemVOList) {
        this.applicationSystemVOList = applicationSystemVOList;
    }

}
