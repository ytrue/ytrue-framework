package com.ytrue.gateway.center.infrastructure.dao;

import com.ytrue.gateway.center.domain.operation.model.vo.GatewayServerDetaiDatalVO;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.po.GatewayServerDetail;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description
 */
@Mapper
public interface IGatewayServerDetailDao {

    void insert(GatewayServerDetail gatewayServerDetail);

    GatewayServerDetail queryGatewayServerDetail(GatewayServerDetail gatewayServerDetail);

    boolean updateGatewayStatus(GatewayServerDetail gatewayServerDetail);

    List<GatewayServerDetail> queryGatewayServerDetailList();

    List<GatewayServerDetail> queryGatewayServerDetailListByPage(OperationRequest<GatewayServerDetaiDatalVO> request);

    int queryGatewayServerDetailListCountByPage(OperationRequest<GatewayServerDetaiDatalVO> request);

}
