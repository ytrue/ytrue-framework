package com.ytrue.gateway.center.application;

import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 接口注册服务
 */
public interface IRegisterManageService {

    void registerApplication(ApplicationSystemVO applicationSystemVO);

    void registerApplicationInterface(ApplicationInterfaceVO applicationInterfaceVO);

    void registerApplicationInterfaceMethod(ApplicationInterfaceMethodVO applicationInterfaceMethodVO);
}
