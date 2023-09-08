package com.ytrue.gateway.center.infrastructure.dao;

import com.ytrue.gateway.center.domain.operation.model.vo.ApplicationSystemDataVO;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.po.ApplicationSystem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 应用系统
 */
@Mapper
public interface IApplicationSystemDao {

    void insert(ApplicationSystem applicationSystem);

    List<ApplicationSystem> queryApplicationSystemList(List<String> list);

    List<ApplicationSystem> queryApplicationSystemListByPage(OperationRequest<ApplicationSystemDataVO> request);

    int queryApplicationSystemListCountByPage(OperationRequest<ApplicationSystemDataVO> request);

}
