package com.ytrue.gateway.center.interfaces;

import com.alibaba.fastjson.JSON;
import com.ytrue.gateway.center.application.IDataOperationManageService;
import com.ytrue.gateway.center.domain.operation.model.vo.*;
import com.ytrue.gateway.center.infrastructure.common.OperationRequest;
import com.ytrue.gateway.center.infrastructure.common.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 网关数据操作管理
 */
@CrossOrigin // @CrossOrigin("https://bugstack.cn")
@RestController
@RequestMapping("/wg/admin/data")
public class DataOperationManage {

    private final Logger logger = LoggerFactory.getLogger(DataOperationManage.class);

    @Resource
    private IDataOperationManageService dataOperationManageService;

    @GetMapping(value = "queryGatewayServer", produces = "application/json;charset=utf-8")
    public OperationResult<GatewayServerDataVO> queryGatewayServer(@RequestParam String groupId,
                                                                   @RequestParam String page,
                                                                   @RequestParam String limit) {
        try {
            logger.info("查询网关服务数据开始 groupId：{} page：{} limit：{}", groupId, page, limit);
            OperationRequest<String> req = new OperationRequest<>(page, limit);
            req.setData(groupId);
            //  SELECT id, group_id, group_name FROM gateway_server
            //        <where>
            //            <if test="null != data">
            //                and group_id = #{data}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<GatewayServerDataVO> operationResult = dataOperationManageService.queryGatewayServer(req);
            logger.info("查询网关服务数据完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询网关服务数据异常 groupId：{}", groupId, e);
            return new OperationResult<>(0, null);
        }
    }

    @GetMapping(value = "queryGatewayServerDetail", produces = "application/json;charset=utf-8")
    public OperationResult<GatewayServerDetaiDatalVO> queryGatewayServerDetail(@RequestParam String groupId,
                                                                               @RequestParam String gatewayId,
                                                                               @RequestParam String page,
                                                                               @RequestParam String limit) {
        try {
            logger.info("查询网关服务详情数据开始 groupId：{} gatewayId：{} page：{} limit：{}", groupId, gatewayId, page, limit);
            OperationRequest<GatewayServerDetaiDatalVO> req = new OperationRequest<>(page, limit);
            req.setData(new GatewayServerDetaiDatalVO(groupId, gatewayId));
            // SELECT id, group_id, gateway_id, gateway_name, gateway_address, status, create_time, update_time
            //        FROM gateway_server_detail
            //        <where>
            //            <if test="data != null and data.groupId != ''">
            //                and group_id = #{data.groupId}
            //            </if>
            //            <if test="data != null and data.gatewayId != ''">
            //                and gateway_id = #{data.gatewayId}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<GatewayServerDetaiDatalVO> operationResult = dataOperationManageService.queryGatewayServerDetail(req);
            logger.info("查询网关服务详情数据完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询网关服务详情数据异常 groupId：{}", groupId, e);
            return new OperationResult<>(0, null);
        }
    }

    @GetMapping(value = "queryGatewayDistribution", produces = "application/json;charset=utf-8")
    public OperationResult<GatewayDistributionDataVO> queryGatewayDistribution(@RequestParam String groupId,
                                                                               @RequestParam String gatewayId,
                                                                               @RequestParam String page,
                                                                               @RequestParam String limit) {
        try {
            logger.info("查询网关分配数据开始 groupId：{} gatewayId：{} page：{} limit：{}", groupId, gatewayId, page, limit);
            OperationRequest<GatewayDistributionDataVO> req = new OperationRequest<>(page, limit);
            req.setData(new GatewayDistributionDataVO(groupId, gatewayId));
            // SELECT id, group_id, gateway_id, system_id, system_name, create_time, update_time
            //        FROM gateway_distribution
            //        <where>
            //            <if test="data != null and data.groupId != ''">
            //                and group_id = #{data.groupId}
            //            </if>
            //            <if test="data != null and data.gatewayId != ''">
            //                and gateway_id = #{data.gatewayId}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<GatewayDistributionDataVO> operationResult = dataOperationManageService.queryGatewayDistribution(req);
            logger.info("查询网关分配数据完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询网关分配数据异常 groupId：{}", groupId, e);
            return new OperationResult<>(0, null);
        }
    }

    @GetMapping(value = "queryApplicationSystem", produces = "application/json;charset=utf-8")
    public OperationResult<ApplicationSystemDataVO> queryApplicationSystem(@RequestParam String systemId,
                                                                           @RequestParam String systemName,
                                                                           @RequestParam String page,
                                                                           @RequestParam String limit) {
        try {
            logger.info("查询应用系统信息开始 systemId：{} systemName：{} page：{} limit：{}", systemId, systemName, page, limit);
            OperationRequest<ApplicationSystemDataVO> req = new OperationRequest<>(page, limit);
            req.setData(new ApplicationSystemDataVO(systemId, systemName));
            //  SELECT id, system_id, system_name, system_type, system_registry
            //        FROM application_system
            //        <where>
            //            <if test="data != null and data.systemId != ''">
            //                and system_id = #{data.systemId}
            //            </if>
            //            <if test="data != null and data.systemName != ''">
            //                and system_name = #{data.systemName}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<ApplicationSystemDataVO> operationResult = dataOperationManageService.queryApplicationSystem(req);
            logger.info("查询应用系统信息完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询应用系统信息异常 systemId：{} systemName：{}", systemId, systemId, e);
            return new OperationResult<>(0, null);
        }
    }

    @GetMapping(value = "queryApplicationInterface", produces = "application/json;charset=utf-8")
    public OperationResult<ApplicationInterfaceDataVO> queryApplicationInterface(@RequestParam String systemId,
                                                                                 @RequestParam String interfaceId,
                                                                                 @RequestParam String page,
                                                                                 @RequestParam String limit) {
        try {
            logger.info("查询应用接口信息开始 systemId：{} interfaceId：{} page：{} limit：{}", systemId, interfaceId, page, limit);
            OperationRequest<ApplicationInterfaceDataVO> req = new OperationRequest<>(page, limit);
            req.setData(new ApplicationInterfaceDataVO(systemId, interfaceId));
            // SELECT id, system_id, interface_id, interface_name, interface_version
            //        FROM application_interface
            //        <where>
            //            <if test="data != null and data.systemId != ''">
            //                and system_id = #{data.systemId}
            //            </if>
            //            <if test="data != null and data.interfaceId != ''">
            //                and interface_id = #{data.interfaceId}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<ApplicationInterfaceDataVO> operationResult = dataOperationManageService.queryApplicationInterface(req);
            logger.info("查询应用接口信息完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询应用接口信息异常 systemId：{} interfaceId：{}", systemId, interfaceId, e);
            return new OperationResult<>(0, null);
        }
    }

    @GetMapping(value = "queryApplicationInterfaceMethodList", produces = "application/json;charset=utf-8")
    public OperationResult<ApplicationInterfaceMethodDataVO> queryApplicationInterfaceMethodList(@RequestParam String systemId,
                                                                                                 @RequestParam String interfaceId,
                                                                                                 @RequestParam String page,
                                                                                                 @RequestParam String limit) {
        try {
            logger.info("查询应用接口方法信息开始 systemId：{} interfaceId：{} page：{} limit：{}", systemId, interfaceId, page, limit);
            OperationRequest<ApplicationInterfaceMethodDataVO> req = new OperationRequest<>(page, limit);
            req.setData(new ApplicationInterfaceMethodDataVO(systemId, interfaceId));
            //  SELECT id, system_id, interface_id, method_id, method_name, parameter_type, uri, http_command_type, auth
            //        FROM application_interface_method
            //        <where>
            //            <if test="data != null and data.systemId != ''">
            //                and system_id = #{data.systemId}
            //            </if>
            //            <if test="data != null and data.interfaceId != ''">
            //                and interface_id = #{data.interfaceId}
            //            </if>
            //        </where>
            //        order by id desc
            //        limit #{pageStart},#{pageEnd}
            OperationResult<ApplicationInterfaceMethodDataVO> operationResult = dataOperationManageService.queryApplicationInterfaceMethod(req);
            logger.info("查询应用接口方法信息完成 operationResult：{}", JSON.toJSONString(operationResult));
            return operationResult;
        } catch (Exception e) {
            logger.error("查询应用接口方法信息异常 systemId：{} interfaceId：{}", systemId, interfaceId, e);
            return new OperationResult<>(0, null);
        }
    }

}
