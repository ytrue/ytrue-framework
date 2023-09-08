package com.ytrue.gateway.center.application;



import com.ytrue.gateway.center.domain.operation.model.vo.*;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.common.OperationResult;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 网关运营数据管理
 */
public interface IDataOperationManageService {

    OperationResult<GatewayServerDataVO> queryGatewayServer(OperationRequest<String> request);

    OperationResult<ApplicationSystemDataVO> queryApplicationSystem(OperationRequest<ApplicationSystemDataVO> request);

    OperationResult<ApplicationInterfaceDataVO> queryApplicationInterface(OperationRequest<ApplicationInterfaceDataVO> request);

    OperationResult<ApplicationInterfaceMethodDataVO> queryApplicationInterfaceMethod(OperationRequest<ApplicationInterfaceMethodDataVO> request);

    OperationResult<GatewayServerDetaiDatalVO> queryGatewayServerDetail(OperationRequest<GatewayServerDetaiDatalVO> request);

    OperationResult<GatewayDistributionDataVO> queryGatewayDistribution(OperationRequest<GatewayDistributionDataVO> request);
}
