package com.ytrue.ioc.core.io;

import cn.hutool.core.lang.Assert;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author ytrue
 * @date 2022/9/30 09:24
 * @description DefaultResourceLoader
 */
public class DefaultResourceLoader implements ResourceLoader {

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        // 前缀有classpath: 就ClassPathResource 读取
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        } else {
            try {
                // 网络读取
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                // 文件读取
                return new FileSystemResource(location);
            }
        }
    }
}
