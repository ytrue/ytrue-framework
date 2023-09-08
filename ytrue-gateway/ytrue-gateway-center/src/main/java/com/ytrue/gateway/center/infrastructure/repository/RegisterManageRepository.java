package com.ytrue.gateway.center.infrastructure.repository;

import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;
import com.ytrue.gateway.center.domain.register.repository.IRegisterManageRepository;
import com.ytrue.gateway.center.infrastructure.dao.IApplicationInterfaceDao;
import com.ytrue.gateway.center.infrastructure.dao.IApplicationInterfaceMethodDao;
import com.ytrue.gateway.center.infrastructure.dao.IApplicationSystemDao;
import com.ytrue.gateway.center.infrastructure.po.ApplicationInterface;
import com.ytrue.gateway.center.infrastructure.po.ApplicationInterfaceMethod;
import com.ytrue.gateway.center.infrastructure.po.ApplicationSystem;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ytrue
 * @date 2023-09-08 14:09
 * @description 接口注册仓储服务
 */
@Component
public class RegisterManageRepository implements IRegisterManageRepository {

    @Resource
    private IApplicationSystemDao applicationSystemDao;
    @Resource
    private IApplicationInterfaceDao applicationInterfaceDao;
    @Resource
    private IApplicationInterfaceMethodDao applicationInterfaceMethodDao;

    @Override
    public void registerApplication(ApplicationSystemVO applicationSystemVO) {
        ApplicationSystem applicationSystem = new ApplicationSystem();
        applicationSystem.setSystemId(applicationSystemVO.getSystemId());
        applicationSystem.setSystemName(applicationSystemVO.getSystemName());
        applicationSystem.setSystemType(applicationSystemVO.getSystemType());
        applicationSystem.setSystemRegistry(applicationSystemVO.getSystemRegistry());
        applicationSystemDao.insert(applicationSystem);
    }

    @Override
    public void registerApplicationInterface(ApplicationInterfaceVO applicationInterfaceVO) {
        ApplicationInterface applicationInterface = new ApplicationInterface();
        applicationInterface.setSystemId(applicationInterfaceVO.getSystemId());
        applicationInterface.setInterfaceId(applicationInterfaceVO.getInterfaceId());
        applicationInterface.setInterfaceName(applicationInterfaceVO.getInterfaceName());
        applicationInterface.setInterfaceVersion(applicationInterfaceVO.getInterfaceVersion());
        applicationInterfaceDao.insert(applicationInterface);
    }

    @Override
    public void registerApplicationInterfaceMethod(ApplicationInterfaceMethodVO applicationInterfaceMethodVO) {
        ApplicationInterfaceMethod applicationInterfaceMethod = new ApplicationInterfaceMethod();
        applicationInterfaceMethod.setSystemId(applicationInterfaceMethodVO.getSystemId());
        applicationInterfaceMethod.setInterfaceId(applicationInterfaceMethodVO.getInterfaceId());
        applicationInterfaceMethod.setMethodId(applicationInterfaceMethodVO.getMethodId());
        applicationInterfaceMethod.setMethodName(applicationInterfaceMethodVO.getMethodName());
        applicationInterfaceMethod.setParameterType(applicationInterfaceMethodVO.getParameterType());
        applicationInterfaceMethod.setUri(applicationInterfaceMethodVO.getUri());
        applicationInterfaceMethod.setHttpCommandType(applicationInterfaceMethodVO.getHttpCommandType());
        applicationInterfaceMethod.setAuth(applicationInterfaceMethodVO.getAuth());
        applicationInterfaceMethodDao.insert(applicationInterfaceMethod);
    }

}
