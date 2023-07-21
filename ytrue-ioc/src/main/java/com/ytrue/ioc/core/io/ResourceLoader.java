package com.ytrue.ioc.core.io;

/**
 * @author ytrue
 * @date 2022/9/30 09:25
 * @description 包装资源加载器
 */
public interface ResourceLoader {

    /**
     * Pseudo URL prefix for loading from the class path: "classpath:"
     */
    String CLASSPATH_URL_PREFIX = "classpath:";

    /**
     * 获取资源
     *
     * @param location
     * @return
     */
    Resource getResource(String location);

}
