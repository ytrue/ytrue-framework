package com.ytrue.gateway.center.interfaces;

import com.ytrue.gateway.center.application.IConfigManageService;
import com.ytrue.gateway.center.application.ILoadBalancingService;
import com.ytrue.gateway.center.application.IMessageService;
import com.ytrue.gateway.center.domain.docker.model.aggregates.NginxConfig;
import com.ytrue.gateway.center.domain.docker.model.vo.LocationVO;
import com.ytrue.gateway.center.domain.docker.model.vo.UpstreamVO;
import com.ytrue.gateway.center.domain.manage.model.aggregates.ApplicationSystemRichInfo;
import com.ytrue.gateway.center.domain.manage.model.vo.*;
import com.ytrue.gateway.center.infrastructure.common.ResponseCode;
import com.ytrue.gateway.center.infrastructure.common.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 网关配置管理；服务分组、网关注册、服务关联
 * 1. 查询网关服务配置项信息：/wg/admin/config/queryServerConfig
 * 2. 注册网关服务节点：/wg/admin/config/registerGateway
 */
@RestController
@RequestMapping("/wg/admin/config")
public class GatewayConfigManage {

    private final Logger logger = LoggerFactory.getLogger(GatewayConfigManage.class);

    @Resource
    private IConfigManageService configManageService;
    @Resource
    private IMessageService messageService;
    @Resource
    private ILoadBalancingService loadBalancingService;

    @GetMapping(value = "queryServerConfig", produces = "application/json;charset=utf-8")
    public Result<List<GatewayServerVO>> queryServerConfig() {
        try {
            logger.info("查询网关服务配置项信息");
            //  SELECT id, group_id, group_name FROM gateway_server
            List<GatewayServerVO> gatewayServerVOList = configManageService.queryGatewayServerList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询网关服务配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @GetMapping(value = "queryServerDetailConfig", produces = "application/json;charset=utf-8")
    public Result<List<GatewayServerDetailVO>> queryServerDetailConfig() {
        try {
            logger.info("查询网关算力节点配置项信息");
            //  SELECT id, group_id, gateway_id, gateway_name, gateway_address, status, create_time, update_time
            //        FROM gateway_server_detail
            List<GatewayServerDetailVO> gatewayServerVOList = configManageService.queryGatewayServerDetailList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询网关算力节点配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @GetMapping(value = "queryGatewayDistributionList", produces = "application/json;charset=utf-8")
    public Result<List<GatewayDistributionVO>> queryGatewayDistributionList() {
        try {
            logger.info("查询网关分配配置项信息");
            //   SELECT id, group_id, gateway_id, system_id, system_name, create_time, update_time
            //        FROM gateway_distribution
            List<GatewayDistributionVO> gatewayServerVOList = configManageService.queryGatewayDistributionList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询网关分配配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    /**
     * 注册网关服务节点
     *
     * @param groupId        分组标识
     * @param gatewayId      网关标识
     * @param gatewayName    网关名称
     * @param gatewayAddress 网关地址
     * @return 注册状态
     */
    @PostMapping(value = "registerGateway", produces = "application/json;charset=utf-8")
    public Result<Boolean> registerGatewayServerNode(@RequestParam String groupId, @RequestParam String gatewayId, @RequestParam String gatewayName, @RequestParam String gatewayAddress) {
        try {
            logger.info("注册网关服务节点 gatewayId：{} gatewayName：{} gatewayAddress：{}", gatewayId, gatewayName, gatewayAddress);
            // 1. 注册&更新网关算力信息
            //   INSERT INTO gateway_server_detail(group_id, gateway_id, gateway_name, gateway_address, status, create_time, update_time)
            //        VALUES (#{groupId}, #{gatewayId}, #{gatewayName}, #{gatewayAddress}, #{status}, NOW(), NOW());
            boolean done = configManageService.registerGatewayServerNode(groupId, gatewayId, gatewayName, gatewayAddress);

            // 2. 读取最新网关算力数据【由于可能来自于多套注册中心，所以从数据库或者Redis中获取，更为准确】
            //   SELECT id, group_id, gateway_id, gateway_name, gateway_address, status, create_time, update_time
            //        FROM gateway_server_detail
            List<GatewayServerDetailVO> gatewayServerDetailVOList = configManageService.queryGatewayServerDetailList();

            // 3. 组装Nginx网关刷新配置信息
            Map<String, List<GatewayServerDetailVO>> gatewayServerDetailMap = gatewayServerDetailVOList.stream()
                    .collect(Collectors.groupingBy(GatewayServerDetailVO::getGroupId));

            // groupIdList [10001,10002 ....]
            Set<String> uniqueGroupIdList = gatewayServerDetailMap.keySet();
            // 3.1 Location 信息
            // [LocationVO{name='/10001/', proxy_pass='http://10001;'}]
            List<LocationVO> locationList = new ArrayList<>();
            // 循环处理
            for (String name : uniqueGroupIdList) {
                // location /api01/ {
                //     rewrite ^/api01/(.*)$ /$1 break;
                // 	   proxy_pass http://api01;
                // }
                locationList.add(new LocationVO("/" + name + "/", "http://" + name + ";"));
            }
            // 3.2 Upstream 信息
            // [UpstreamVO{name='10001', strategy='least_conn;', servers=[172.20.10.12:7397]}]
            List<UpstreamVO> upstreamList = new ArrayList<>();
            for (String name : uniqueGroupIdList) {
                // upstream api01 {
                //     least_conn;
                //     server 172.20.10.12:9001;
                //     #server 172.20.10.12:9002;
                // }
                List<String> servers = gatewayServerDetailMap.get(name).stream()
                        .map(GatewayServerDetailVO::getGatewayAddress)
                        .collect(Collectors.toList());
                upstreamList.add(new UpstreamVO(name, "least_conn;", servers));
            }
            // 4. 刷新Nginx配置
            loadBalancingService.updateNginxConfig(new NginxConfig(upstreamList, locationList));
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), done);
        } catch (Exception e) {
            logger.error("注册网关服务节点异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), false);
        }
    }

    /**
     * TODO 开发完成续应用注册后，开发这部分
     */
    @PostMapping(value = "distributionGateway", produces = "application/json;charset=utf-8")
    public void distributionGatewayServerNode(@RequestParam String groupId, @RequestParam String gatewayId) {

    }

    @PostMapping(value = "queryApplicationSystemList", produces = "application/json;charset=utf-8")
    public Result<List<ApplicationSystemVO>> queryApplicationSystemList() {
        try {
            logger.info("查询应用服务配置项信息");
            //    SELECT id, system_id, system_name, system_type, system_registry
            //        FROM application_system
            //        <where>
            //            <if test="list != null">
            //                system_id in
            //                <foreach collection="list" index="idx" item="ids" open="(" close=")" separator=",">
            //                    #{ids}
            //                </foreach>
            //            </if>
            //        </where>
            List<ApplicationSystemVO> gatewayServerVOList = configManageService.queryApplicationSystemList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询应用服务配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @PostMapping(value = "queryApplicationInterfaceList", produces = "application/json;charset=utf-8")
    public Result<List<ApplicationInterfaceVO>> queryApplicationInterfaceList() {
        try {
            logger.info("查询应用接口配置项信息");
            //  SELECT id, system_id, interface_id, method_id, method_name, parameter_type, uri, http_command_type, auth
            //        FROM application_interface_method
            //        <where>
            //            <if test="systemId != null">
            //                system_id = #{systemId}
            //            </if>
            //            <if test="systemId != null">
            //                AND interface_id = #{interfaceId}
            //            </if>
            //        </where>
            List<ApplicationInterfaceVO> gatewayServerVOList = configManageService.queryApplicationInterfaceList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询应用接口配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @PostMapping(value = "queryApplicationInterfaceMethodList", produces = "application/json;charset=utf-8")
    public Result<List<ApplicationInterfaceMethodVO>> queryApplicationInterfaceMethodList() {
        try {
            logger.info("查询应用接口方法配置项信息");
            // SELECT id, system_id, interface_id, method_id, method_name, parameter_type, uri, http_command_type, auth
            //        FROM application_interface_method
            //        <where>
            //            <if test="systemId != null">
            //                system_id = #{systemId}
            //            </if>
            //            <if test="systemId != null">
            //                AND interface_id = #{interfaceId}
            //            </if>
            //        </where>
            List<ApplicationInterfaceMethodVO> gatewayServerVOList = configManageService.queryApplicationInterfaceMethodList();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), gatewayServerVOList);
        } catch (Exception e) {
            logger.error("查询应用接口方法配置项信息异常", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @PostMapping(value = "queryApplicationSystemRichInfo", produces = "application/json;charset=utf-8")
    public Result<ApplicationSystemRichInfo> queryApplicationSystemRichInfo(@RequestParam String gatewayId,
                                                                            @RequestParam String systemId) {
        try {
            logger.info("查询分配到网关下的待注册系统信息(系统、接口、方法) gatewayId：{}", gatewayId);
            // {
            //  "gatewayId": "api-gateway-g4",
            //  "applicationSystemVOList": [
            //    {
            //      "systemId": "api-gateway-test-provider",
            //      "systemName": "网关sdk测试工程",
            //      "systemRegistry": "zookeeper://172.20.10.12:2181",
            //      "systemType": "RPC",
            //      "interfaceList": [
            //        {
            //          "interfaceId": "cn.bugstack.gateway.rpc.IActivityBooth",
            //          "interfaceName": "活动服务",
            //          "interfaceVersion": "1.0.0",
            //          "systemId": "api-gateway-test-provider",
            //          "methodList": [
            //            {
            //              "auth": 1,
            //              "httpCommandType": "POST",
            //              "interfaceId": "cn.bugstack.gateway.rpc.IActivityBooth",
            //              "methodId": "insert",
            //              "methodName": "插入方法",
            //              "parameterType": "cn.bugstack.gateway.rpc.dto.XReq",
            //              "systemId": "api-gateway-test-provider",
            //              "uri": "/wg/activity/insert"
            //            },
            //            {
            //              "auth": 0,
            //              "httpCommandType": "GET",
            //              "interfaceId": "cn.bugstack.gateway.rpc.IActivityBooth",
            //              "methodId": "sayHi",
            //              "methodName": "探活方法",
            //              "parameterType": "java.lang.String",
            //              "systemId": "api-gateway-test-provider",
            //              "uri": "/wg/activity/sayHi"
            //            },
            //            {
            //              "auth": 0,
            //              "httpCommandType": "POST",
            //              "interfaceId": "cn.bugstack.gateway.rpc.IActivityBooth",
            //              "methodId": "test",
            //              "methodName": "测试方法",
            //              "parameterType": "java.lang.String,cn.bugstack.gateway.rpc.dto.XReq",
            //              "systemId": "api-gateway-test-provider",
            //              "uri": "/wg/activity/test"
            //            }
            //          ]
            //        }
            //      ]
            //    }
            //  ]
            //}
            ApplicationSystemRichInfo applicationSystemRichInfo = configManageService.queryApplicationSystemRichInfo(gatewayId, systemId);
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), applicationSystemRichInfo);
        } catch (Exception e) {
            logger.error("查询分配到网关下的待注册系统信息(系统、接口、方法)异常 gatewayId：{}", gatewayId, e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

    @PostMapping(value = "queryRedisConfig", produces = "application/json;charset=utf-8")
    public Result<Map<String, String>> queryRedisConfig() {
        try {
            logger.info("查询配置中心Redis配置信息");
            //  return new HashMap<String, String>() {{
            //            put("host", host);
            //            put("port", String.valueOf(port));
            //        }};
            Map<String, String> redisConfig = messageService.queryRedisConfig();
            return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getInfo(), redisConfig);
        } catch (Exception e) {
            logger.error("查询配置中心Redis配置信息失败", e);
            return new Result<>(ResponseCode.UN_ERROR.getCode(), e.getMessage(), null);
        }
    }

}
