package com.ytrue.gateway.center.interfaces;

import com.ytrue.gateway.center.application.IConfigManageService;
import com.ytrue.gateway.center.application.IMessageService;
import com.ytrue.gateway.center.application.IRegisterManageService;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;
import com.ytrue.gateway.center.infrastructure.common.ResponseCode;
import com.ytrue.gateway.center.infrastructure.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description RPC 服务注册管理
 */
@RestController
@RequestMapping("/wg/admin/register")
public class RpcRegisterManage {

    private final Logger logger = LoggerFactory.getLogger(RpcRegisterManage.class);

    @Resource
    private IRegisterManageService registerManageService;
    @Resource
    private IConfigManageService configManageService;
    @Resource
    private IMessageService messageService;

    @PostMapping(value = "registerApplication", produces = "application/json;charset=utf-8")
    public Result<Boolean> registerApplication(@RequestParam String systemId,
                                               @RequestParam String systemName,
                                               @RequestParam String systemType,
                                               @RequestParam String systemRegistry) {
        try {
            logger.info("注册应用服务 systemId：{}", systemId);
            ApplicationSystemVO applicationSystemVO = new ApplicationSystemVO();
            applicationSystemVO.setSystemId(systemId);
            applicationSystemVO.setSystemName(systemName);
            applicationSystemVO.setSystemType(systemType);
            applicationSystemVO.setSystemRegistry(systemRegistry);

            //     INSERT INTO application_system(system_id, system_name, system_type, system_registry, create_time, update_time)
            //        VALUES (#{systemId}, #{systemName}, #{systemType}, #{systemRegistry}, NOW(), NOW());
            registerManageService.registerApplication(applicationSystemVO);
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), true);
        } catch (DuplicateKeyException e) {
            logger.warn("注册应用服务重复 systemId：{}", systemId, e);
            return new Result<>(ResponseCode.INDEX_DUP.getCode(), e.getMessage(), true);
        } catch (Exception e) {
            logger.error("注册应用服务失败 systemId：{}", systemId, e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), false);
        }
    }

    @PostMapping(value = "registerApplicationInterface", produces = "application/json;charset=utf-8")
    public Result<Boolean> registerApplicationInterface(@RequestParam String systemId,
                                                        @RequestParam String interfaceId,
                                                        @RequestParam String interfaceName,
                                                        @RequestParam String interfaceVersion) {
        try {
            logger.info("注册应用接口 systemId：{} interfaceId：{}", systemId, interfaceId);
            ApplicationInterfaceVO applicationInterfaceVO = new ApplicationInterfaceVO();
            applicationInterfaceVO.setSystemId(systemId);
            applicationInterfaceVO.setInterfaceId(interfaceId);
            applicationInterfaceVO.setInterfaceName(interfaceName);
            applicationInterfaceVO.setInterfaceVersion(interfaceVersion);

            //   INSERT INTO application_interface(system_id, interface_id, interface_name, interface_version, create_time, update_time)
            //        VALUES (#{systemId}, #{interfaceId}, #{interfaceName}, #{interfaceVersion}, NOW(), NOW());
            registerManageService.registerApplicationInterface(applicationInterfaceVO);
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), true);
        } catch (DuplicateKeyException e) {
            logger.warn("注册应用接口重复 systemId：{} interfaceId：{}", systemId, interfaceId);
            return new Result<>(ResponseCode.INDEX_DUP.getCode(), e.getMessage(), true);
        } catch (Exception e) {
            logger.error("注册应用接口失败 systemId：{}", systemId, e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), false);
        }
    }

    @PostMapping(value = "registerApplicationInterfaceMethod", produces = "application/json;charset=utf-8")
    public Result<Boolean> registerApplicationInterfaceMethod(@RequestParam String systemId,
                                                              @RequestParam String interfaceId,
                                                              @RequestParam String methodId,
                                                              @RequestParam String methodName,
                                                              @RequestParam String parameterType,
                                                              @RequestParam String uri,
                                                              @RequestParam String httpCommandType,
                                                              @RequestParam Integer auth) {
        try {
            logger.info("注册应用接口方法 systemId：{} interfaceId：{} methodId：{}", systemId, interfaceId, methodId);
            ApplicationInterfaceMethodVO applicationInterfaceMethodVO = new ApplicationInterfaceMethodVO();
            applicationInterfaceMethodVO.setSystemId(systemId);
            applicationInterfaceMethodVO.setInterfaceId(interfaceId);
            applicationInterfaceMethodVO.setMethodId(methodId);
            applicationInterfaceMethodVO.setMethodName(methodName);
            applicationInterfaceMethodVO.setParameterType(parameterType);
            applicationInterfaceMethodVO.setUri(uri);
            applicationInterfaceMethodVO.setHttpCommandType(httpCommandType);
            applicationInterfaceMethodVO.setAuth(auth);

            //    INSERT INTO application_interface_method(system_id, interface_id, method_id, method_name, parameter_type,
            //        uri, http_command_type, auth, create_time, update_time)
            //        VALUES (#{systemId}, #{interfaceId}, #{methodId}, #{methodName}, #{parameterType},
            //                #{uri}, #{httpCommandType}, #{auth}, NOW(), NOW());
            registerManageService.registerApplicationInterfaceMethod(applicationInterfaceMethodVO);
            // 推送注册消息
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), true);
        } catch (DuplicateKeyException e) {
            logger.warn("注册应用接口重复 systemId：{} interfaceId：{}", systemId, interfaceId);
            return new Result<>(ResponseCode.INDEX_DUP.getCode(), e.getMessage(), true);
        } catch (Exception e) {
            logger.error("注册应用接口失败 systemId：{}", systemId, e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), false);
        }
    }

    @PostMapping(value = "registerEvent", produces = "application/json;charset=utf-8")
    public Result<Boolean> registerEvent(@RequestParam String systemId) {
        try {
            logger.info("应用信息注册完成通知 systemId：{}", systemId);
            // 推送注册消息
            String gatewayId = configManageService.queryGatewayDistribution(systemId);

            // redis发布订阅
            //       redisMessageTemplate.convertAndSend(topic, message);
            messageService.pushMessage(gatewayId, systemId);
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), true);
        } catch (Exception e) {
            logger.error("应用信息注册完成通知失败 systemId：{}", systemId, e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), false);
        }
    }

}
