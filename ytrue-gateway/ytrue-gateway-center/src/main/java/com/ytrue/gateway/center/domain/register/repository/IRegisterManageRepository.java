package com.ytrue.gateway.center.domain.register.repository;

import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 接口注册仓储服务
 */
public interface IRegisterManageRepository {

    void registerApplication(ApplicationSystemVO applicationSystemVO);

    void registerApplicationInterface(ApplicationInterfaceVO applicationInterfaceVO);

    void registerApplicationInterfaceMethod(ApplicationInterfaceMethodVO applicationInterfaceMethodVO);

}
