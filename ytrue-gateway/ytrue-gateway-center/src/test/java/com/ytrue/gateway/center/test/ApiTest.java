package com.ytrue.gateway.center.test;

import com.ytrue.gateway.center.ApiGatewayApplication;
import com.ytrue.gateway.center.application.IConfigManageService;
import com.ytrue.gateway.center.application.IRegisterManageService;
import com.ytrue.gateway.center.domain.docker.model.vo.LocationVO;
import com.ytrue.gateway.center.domain.docker.model.vo.UpstreamVO;
import com.ytrue.gateway.center.domain.manage.model.aggregates.ApplicationSystemRichInfo;
import com.ytrue.gateway.center.domain.manage.model.vo.GatewayDistributionVO;
import com.ytrue.gateway.center.domain.manage.model.vo.GatewayServerDetailVO;
import com.ytrue.gateway.center.domain.manage.model.vo.GatewayServerVO;
import com.ytrue.gateway.center.domain.message.Publisher;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceMethodVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationInterfaceVO;
import com.ytrue.gateway.center.domain.register.model.vo.ApplicationSystemVO;
import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ytrue
 * @date 2023-09-08 14:07
 * @description 单元测试
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiGatewayApplication.class)
public class ApiTest {

    private Logger logger = LoggerFactory.getLogger(ApiTest.class);

    @Resource
    private IConfigManageService configManageService;

    @Resource
    private IRegisterManageService registerManageService;

    @Test
    public void test_queryGatewayServerList() {
        List<GatewayServerVO> gatewayServerVOS = configManageService.queryGatewayServerList();
        logger.info("测试结果：{}", JSON.toJSONString(gatewayServerVOS));
    }

    @Test
    public void test_queryGatewayServerDetailList() {
        List<GatewayServerDetailVO> gatewayServerVOS = configManageService.queryGatewayServerDetailList();
        logger.info("测试结果：{}", JSON.toJSONString(gatewayServerVOS));
    }

    @Test
    public void test_queryGatewayDistributionList() {
        List<GatewayDistributionVO> gatewayDistributionVOList = configManageService.queryGatewayDistributionList();
        logger.info("测试结果：{}", JSON.toJSONString(gatewayDistributionVOList));
    }

    @Test
    public void test_application() {
        logger.info("测试结果：{}", JSON.toJSONString(configManageService.queryApplicationSystemList()));
        logger.info("测试结果：{}", JSON.toJSONString(configManageService.queryApplicationInterfaceList()));
        logger.info("测试结果：{}", JSON.toJSONString(configManageService.queryApplicationInterfaceMethodList()));
    }

    @Test
    public void test_registerGatewayServerNode() {
        configManageService.registerGatewayServerNode("10001", "api-gateway-g4", "电商支付网关", "172.20.10.12:7399");
    }

    @Test
    public void test_registerApplication() {
        ApplicationSystemVO applicationSystemVO = new ApplicationSystemVO();
        applicationSystemVO.setSystemId("api-gateway-test");
        applicationSystemVO.setSystemName("网关测试系统");
        applicationSystemVO.setSystemType("RPC");
        applicationSystemVO.setSystemRegistry("127.0.0.1");
        registerManageService.registerApplication(applicationSystemVO);
    }

    @Test
    public void test_registerApplicationInterface() {
        ApplicationInterfaceVO applicationInterfaceVO = new ApplicationInterfaceVO();
        applicationInterfaceVO.setSystemId("api-gateway-test");
        applicationInterfaceVO.setInterfaceId("com.ytrue.gateway.rpc.IActivityBooth");
        applicationInterfaceVO.setInterfaceName("活动平台");
        applicationInterfaceVO.setInterfaceVersion("v1.0.0");
        registerManageService.registerApplicationInterface(applicationInterfaceVO);
    }

    @Test
    public void test_registerApplicationInterfaceMethod() {
        ApplicationInterfaceMethodVO applicationInterfaceVO01 = new ApplicationInterfaceMethodVO();
        applicationInterfaceVO01.setSystemId("api-gateway-test");
        applicationInterfaceVO01.setInterfaceId("com.ytrue.gateway.rpc.IActivityBooth");
        applicationInterfaceVO01.setMethodId("sayHi");
        applicationInterfaceVO01.setMethodName("测试方法");
        applicationInterfaceVO01.setParameterType("java.lang.String");
        applicationInterfaceVO01.setUri("/wg/activity/sayHi");
        applicationInterfaceVO01.setHttpCommandType("GET");
        applicationInterfaceVO01.setAuth(0);
        registerManageService.registerApplicationInterfaceMethod(applicationInterfaceVO01);

        ApplicationInterfaceMethodVO applicationInterfaceVO02 = new ApplicationInterfaceMethodVO();
        applicationInterfaceVO02.setSystemId("api-gateway-test");
        applicationInterfaceVO02.setInterfaceId("com.ytrue.gateway.rpc.IActivityBooth");
        applicationInterfaceVO02.setMethodId("insert");
        applicationInterfaceVO02.setMethodName("插入方法");
        applicationInterfaceVO02.setParameterType("com.ytrue.gateway.rpc.dto.XReq");
        applicationInterfaceVO02.setUri("/wg/activity/insert");
        applicationInterfaceVO02.setHttpCommandType("POST");
        applicationInterfaceVO02.setAuth(1);
        registerManageService.registerApplicationInterfaceMethod(applicationInterfaceVO02);
    }

    @Test
    public void test_queryApplicationSystemRichInfo() {
//        ApplicationSystemRichInfo result = configManageService.queryApplicationSystemRichInfo("api-gateway-g4", "");
//        logger.info("测试结果：{}", JSON.toJSONString(result));

        List<GatewayServerDetailVO> gatewayServerDetailVOList = configManageService.queryGatewayServerDetailList();

        // 3. 组装Nginx网关刷新配置信息
        Map<String, List<GatewayServerDetailVO>> gatewayServerDetailMap = gatewayServerDetailVOList.stream()
                .collect(Collectors.groupingBy(GatewayServerDetailVO::getGroupId));

        // groupIdList
        Set<String> uniqueGroupIdList = gatewayServerDetailMap.keySet();

        List<LocationVO> locationList = new ArrayList<>();
        // 循环处理
        for (String name : uniqueGroupIdList) {
            // location /api01/ {
            //     rewrite ^/api01/(.*)$ /$1 break;
            // 	   proxy_pass http://api01;
            // }
            locationList.add(new LocationVO("/" + name + "/", "http://" + name + ";"));
        }

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

        System.out.println(upstreamList);
    }

    @Resource
    private Publisher publisher;

    @Test
    public void test_messages() throws InterruptedException {
        publisher.pushMessage("api-gateway-g4", "api-gateway-test-provider");
        Thread.sleep(50000);
    }

}
