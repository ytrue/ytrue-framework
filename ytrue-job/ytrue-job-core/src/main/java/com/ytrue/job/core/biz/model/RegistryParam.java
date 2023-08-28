package com.ytrue.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author ytrue
 * @date 2023-08-28 11:05
 * @description 注册执行器到调度中心时发送的注册参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistryParam implements Serializable {

    private static final long serialVersionUID = 42L;

    /**
     * 注册方式
     */
    private String registryGroup;
    /**
     * 执行器的注册名称
     */
    private String registryKey;
    /**
     * 执行器的地址
     */
    private String registryValue;
}
