package com.ytrue.gateway.center.infrastructure.dao;

import com.ytrue.gateway.center.domain.operation.model.vo.GatewayDistributionDataVO;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.po.GatewayDistribution;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 网关分配
 */
@Mapper
public interface IGatewayDistributionDao {

    List<String> queryGatewayDistributionSystemIdList(String gatewayId);

    String queryGatewayDistribution(String systemId);

    List<GatewayDistribution> queryGatewayDistributionList();

    List<GatewayDistribution> queryGatewayDistributionListByPage(OperationRequest<GatewayDistributionDataVO> request);

    int queryGatewayDistributionListCountByPage(OperationRequest<GatewayDistributionDataVO> request);

}
