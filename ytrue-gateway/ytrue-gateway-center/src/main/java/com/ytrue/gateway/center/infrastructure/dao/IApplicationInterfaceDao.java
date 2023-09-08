package com.ytrue.gateway.center.infrastructure.dao;

import com.ytrue.gateway.center.domain.operation.model.vo.ApplicationInterfaceDataVO;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.po.ApplicationInterface;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 应用接口
 */
@Mapper
public interface IApplicationInterfaceDao {

    void insert(ApplicationInterface applicationInterface);

    List<ApplicationInterface> queryApplicationInterfaceList(String systemId);

    List<ApplicationInterface> queryApplicationInterfaceListByPage(OperationRequest<ApplicationInterfaceDataVO> request);

    int queryApplicationInterfaceListCountByPage(OperationRequest<ApplicationInterfaceDataVO> request);

}
