package com.ytrue.gateway.center.infrastructure.repository;

import com.ytrue.gateway.center.domain.operation.model.vo.*;
import com.ytrue.gateway.center.domain.operation.repository.IDataOperationManageRepository;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.dao.*;
import com.ytrue.gateway.center.infrastructure.po.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description
 */
@Component
public class DataOperationManageRepository implements IDataOperationManageRepository {

    @Resource
    private IGatewayServerDao gatewayServerDao;
    @Resource
    private IGatewayServerDetailDao gatewayServerDetailDao;
    @Resource
    private IGatewayDistributionDao gatewayDistributionDao;
    @Resource
    private IApplicationSystemDao applicationSystemDao;
    @Resource
    private IApplicationInterfaceDao applicationInterfaceDao;
    @Resource
    private IApplicationInterfaceMethodDao applicationInterfaceMethodDao;

    @Override
    public List<GatewayServerDataVO> queryGatewayServerListByPage(OperationRequest<String> request) {
        List<GatewayServer> gatewayServers = gatewayServerDao.queryGatewayServerListByPage(request);
        List<GatewayServerDataVO> gatewayServerVOList = new ArrayList<>(gatewayServers.size());
        for (GatewayServer gatewayServer : gatewayServers) {
            // 可以按照 IDEA 插件 vo2dto 方便转换
            GatewayServerDataVO gatewayServerVO = new GatewayServerDataVO();
            gatewayServerVO.setId(gatewayServer.getId());
            gatewayServerVO.setGroupId(gatewayServer.getGroupId());
            gatewayServerVO.setGroupName(gatewayServer.getGroupName());
            gatewayServerVOList.add(gatewayServerVO);
        }
        return gatewayServerVOList;
    }

    @Override
    public int queryGatewayServerListCountByPage(OperationRequest<String> request) {
        return gatewayServerDao.queryGatewayServerListCountByPage(request);
    }

    @Override
    public List<ApplicationSystemDataVO> queryApplicationSystemListByPage(OperationRequest<ApplicationSystemDataVO> request) {
        List<ApplicationSystem> applicationSystems = applicationSystemDao.queryApplicationSystemListByPage(request);
        List<ApplicationSystemDataVO> applicationSystemDataVOList = new ArrayList<>(applicationSystems.size());
        for (ApplicationSystem applicationSystem : applicationSystems) {
            ApplicationSystemDataVO applicationSystemDataVO = new ApplicationSystemDataVO();
            applicationSystemDataVO.setSystemId(applicationSystem.getSystemId());
            applicationSystemDataVO.setSystemName(applicationSystem.getSystemName());
            applicationSystemDataVO.setSystemType(applicationSystem.getSystemType());
            applicationSystemDataVO.setSystemRegistry(applicationSystem.getSystemRegistry());
            applicationSystemDataVOList.add(applicationSystemDataVO);
        }
        return applicationSystemDataVOList;
    }

    @Override
    public int queryApplicationSystemListCountByPage(OperationRequest<ApplicationSystemDataVO> request) {
        return applicationSystemDao.queryApplicationSystemListCountByPage(request);
    }

    @Override
    public List<ApplicationInterfaceDataVO> queryApplicationInterfaceListByPage(OperationRequest<ApplicationInterfaceDataVO> request) {
        List<ApplicationInterface> applicationInterfaces = applicationInterfaceDao.queryApplicationInterfaceListByPage(request);
        List<ApplicationInterfaceDataVO> applicationInterfaceDataVOList = new ArrayList<>(applicationInterfaces.size());
        for (ApplicationInterface applicationInterface : applicationInterfaces) {
            ApplicationInterfaceDataVO applicationInterfaceDataVO = new ApplicationInterfaceDataVO();
            applicationInterfaceDataVO.setSystemId(applicationInterface.getSystemId());
            applicationInterfaceDataVO.setInterfaceId(applicationInterface.getInterfaceId());
            applicationInterfaceDataVO.setInterfaceName(applicationInterface.getInterfaceName());
            applicationInterfaceDataVO.setInterfaceVersion(applicationInterface.getInterfaceVersion());
            applicationInterfaceDataVOList.add(applicationInterfaceDataVO);
        }
        return applicationInterfaceDataVOList;
    }

    @Override
    public int queryApplicationInterfaceListCountByPage(OperationRequest<ApplicationInterfaceDataVO> request) {
        return applicationInterfaceDao.queryApplicationInterfaceListCountByPage(request);
    }

    @Override
    public List<ApplicationInterfaceMethodDataVO> queryApplicationInterfaceMethodListByPage(OperationRequest<ApplicationInterfaceMethodDataVO> request) {
        List<ApplicationInterfaceMethod> applicationInterfaceMethods = applicationInterfaceMethodDao.queryApplicationInterfaceMethodListByPage(request);
        List<ApplicationInterfaceMethodDataVO> applicationInterfaceMethodDataVOList = new ArrayList<>(applicationInterfaceMethods.size());
        for (ApplicationInterfaceMethod applicationInterfaceMethod : applicationInterfaceMethods) {
            ApplicationInterfaceMethodDataVO applicationInterfaceMethodDataVO = new ApplicationInterfaceMethodDataVO();
            applicationInterfaceMethodDataVO.setSystemId(applicationInterfaceMethod.getSystemId());
            applicationInterfaceMethodDataVO.setInterfaceId(applicationInterfaceMethod.getInterfaceId());
            applicationInterfaceMethodDataVO.setMethodId(applicationInterfaceMethod.getMethodId());
            applicationInterfaceMethodDataVO.setMethodName(applicationInterfaceMethod.getMethodName());
            applicationInterfaceMethodDataVO.setParameterType(applicationInterfaceMethod.getParameterType());
            applicationInterfaceMethodDataVO.setUri(applicationInterfaceMethod.getUri());
            applicationInterfaceMethodDataVO.setHttpCommandType(applicationInterfaceMethod.getHttpCommandType());
            applicationInterfaceMethodDataVO.setAuth(applicationInterfaceMethod.getAuth());
            applicationInterfaceMethodDataVOList.add(applicationInterfaceMethodDataVO);
        }
        return applicationInterfaceMethodDataVOList;
    }

    @Override
    public int queryApplicationInterfaceMethodListCountByPage(OperationRequest<ApplicationInterfaceMethodDataVO> request) {
        return applicationInterfaceMethodDao.queryApplicationInterfaceMethodListCountByPage(request);
    }

    @Override
    public List<GatewayServerDetaiDatalVO> queryGatewayServerDetailListByPage(OperationRequest<GatewayServerDetaiDatalVO> request) {
        List<GatewayServerDetail> applicationInterfaceMethods = gatewayServerDetailDao.queryGatewayServerDetailListByPage(request);
        List<GatewayServerDetaiDatalVO> gatewayServerDetailDataVOList = new ArrayList<>(applicationInterfaceMethods.size());
        for (GatewayServerDetail gatewayServerDetail : applicationInterfaceMethods) {
            GatewayServerDetaiDatalVO gatewayServerDetaiDatalVO = new GatewayServerDetaiDatalVO();
            gatewayServerDetaiDatalVO.setId(gatewayServerDetail.getId());
            gatewayServerDetaiDatalVO.setGroupId(gatewayServerDetail.getGroupId());
            gatewayServerDetaiDatalVO.setGatewayId(gatewayServerDetail.getGatewayId());
            gatewayServerDetaiDatalVO.setGatewayName(gatewayServerDetail.getGatewayName());
            gatewayServerDetaiDatalVO.setGatewayAddress(gatewayServerDetail.getGatewayAddress());
            gatewayServerDetaiDatalVO.setStatus(gatewayServerDetail.getStatus());
            gatewayServerDetaiDatalVO.setCreateTime(gatewayServerDetail.getCreateTime());
            gatewayServerDetaiDatalVO.setUpdateTime(gatewayServerDetail.getUpdateTime());
            gatewayServerDetailDataVOList.add(gatewayServerDetaiDatalVO);
        }
        return gatewayServerDetailDataVOList;
    }

    @Override
    public int queryGatewayServerDetailListCountByPage(OperationRequest<GatewayServerDetaiDatalVO> request) {
        return gatewayServerDetailDao.queryGatewayServerDetailListCountByPage(request);
    }

    @Override
    public List<GatewayDistributionDataVO> queryGatewayDistributionListByPage(OperationRequest<GatewayDistributionDataVO> request) {
        List<GatewayDistribution> gatewayDistributions = gatewayDistributionDao.queryGatewayDistributionListByPage(request);
        List<GatewayDistributionDataVO> gatewayServerDetailDataVOList = new ArrayList<>(gatewayDistributions.size());
        for (GatewayDistribution gatewayDistribution : gatewayDistributions) {
            GatewayDistributionDataVO gatewayDistributionDataVO = new GatewayDistributionDataVO();
            gatewayDistributionDataVO.setId(gatewayDistribution.getId());
            gatewayDistributionDataVO.setGroupId(gatewayDistribution.getGroupId());
            gatewayDistributionDataVO.setGatewayId(gatewayDistribution.getGatewayId());
            gatewayDistributionDataVO.setSystemId(gatewayDistribution.getSystemId());
            gatewayDistributionDataVO.setSystemName(gatewayDistribution.getSystemName());
            gatewayDistributionDataVO.setCreateTime(gatewayDistribution.getCreateTime());
            gatewayDistributionDataVO.setUpdateTime(gatewayDistribution.getUpdateTime());
            gatewayServerDetailDataVOList.add(gatewayDistributionDataVO);
        }
        return gatewayServerDetailDataVOList;
    }

    @Override
    public int queryGatewayDistributionListCountByPage(OperationRequest<GatewayDistributionDataVO> request) {
        return gatewayDistributionDao.queryGatewayDistributionListCountByPage(request);
    }

}
