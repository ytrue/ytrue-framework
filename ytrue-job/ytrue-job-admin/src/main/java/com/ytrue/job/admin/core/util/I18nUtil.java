package com.ytrue.job.admin.core.util;

import com.ytrue.job.admin.core.conf.XxlJobAdminConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author ytrue
 * @date 2023-08-29 9:38
 * @description I18nUtil
 */
public class I18nUtil {

    private static final Logger logger = LoggerFactory.getLogger(I18nUtil.class);

    private static Properties prop = null;

    /**
     * 读取I18的配置文件
     *
     * @return
     */
    public static Properties loadI18nProp() {
        if (prop != null) {
            return prop;
        }
        try {
            //这里是从用户自己定义的配置文件中得到I18的zh_CN
            String i18n = XxlJobAdminConfig.getAdminConfig().getI18n();
            //然后获得要选取的I18的对应的文件路径
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);
            //根据路径创建Resource
            Resource resource = new ClassPathResource(i18nFile);
            //编码
            EncodedResource encodedResource = new EncodedResource(resource, "UTF-8");
            //加载I18的文件
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
        return prop;
    }


    /**
     * 根据Key获取value
     *
     * @param key
     * @return
     */
    public static String getString(String key) {
        return loadI18nProp().getProperty(key);
    }


    /**
     * 获取多个
     *
     * @param keys
     * @return
     */
    public static String getMultString(String... keys) {
        Map<String, String> map = new HashMap<String, String>();
        Properties prop = loadI18nProp();
        if (keys != null && keys.length > 0) {
            for (String key : keys) {
                map.put(key, prop.getProperty(key));
            }
        } else {
            for (String key : prop.stringPropertyNames()) {
                map.put(key, prop.getProperty(key));
            }
        }
        return JacksonUtil.writeValueAsString(map);
    }
}
