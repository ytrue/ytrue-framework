package com.ytrue.job.admin.core.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author ytrue
 * @date 2023-08-29 9:44
 * @description 执行器组的实体类，实际上就是把执行相同定时任务的执行器用这个对象来封装
 * 因为执行相同定时任务的执行器除了服务实例地址不同，其他的都相同，所以可以放到一起，服务实例地址用逗号隔开即可
 * 该实体类对应的就是数据库中的xxl-job-group这张表
 */
@Data
public class XxlJobGroup {

    private int id;
    /**
     * 执行器中配置的项目名称
     */
    private String appname;
    /**
     * 中文名称
     */
    private String title;
    /**
     * 执行器的注册方法，0为自动注册，1为手动注册
     * 这里其实很容易理解，web界面是可以手动录入执行器地址的，同时启动执行器
     * 执行器也会自动将自己注册到调度中心的服务器中
     */
    private int addressType;
    /**
     * 执行器的地址，地址为IP+Port，不同的地址用逗号分开
     */
    private String addressList;
    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 这里实际上就是把addressList属性中的多个地址转变成list集合了，集合中存储的就是注册的所有执行器地址
     */
    private List<String> registryList;

    /**
     * 该方法会讲addressList属性中的所有地址转变成list集合
     *
     * @return
     */
    public List<String> getRegistryList() {
        if (addressList != null && addressList.trim().length() > 0) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(",")));
        }
        return registryList;
    }


}
