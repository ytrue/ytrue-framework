package com.ytrue.gateway.center.domain.register.service;

import com.ytrue.gateway.center.application.IRegisterManageService;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;
import com.ytrue.gateway.center.domain.register.repository.IRegisterManageRepository;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;


/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 接口注册服务
 */
@Service
public class IRegisterManageServiceImpl implements IRegisterManageService {

    @Resource
    private IRegisterManageRepository registerManageRepository;

    @Override
    public void registerApplication(ApplicationSystemVO applicationSystemVO) {
        registerManageRepository.registerApplication(applicationSystemVO);
    }

    @Override
    public void registerApplicationInterface(ApplicationInterfaceVO applicationInterfaceVO) {
        registerManageRepository.registerApplicationInterface(applicationInterfaceVO);
    }

    @Override
    public void registerApplicationInterfaceMethod(ApplicationInterfaceMethodVO applicationInterfaceMethodVO) {
        registerManageRepository.registerApplicationInterfaceMethod(applicationInterfaceMethodVO);
    }

}
