package com.ytrue.gateway.center.infrastructure.dao;

import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.po.GatewayServer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description
 */
@Mapper
public interface IGatewayServerDao {

    List<GatewayServer> queryGatewayServerList();

    List<GatewayServer> queryGatewayServerListByPage(OperationRequest<String> request);

    int queryGatewayServerListCountByPage(OperationRequest<String> request);

}
