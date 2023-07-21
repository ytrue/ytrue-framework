package com.ytrue.ioc.core.io;

import cn.hutool.core.lang.Assert;
import com.ytrue.ioc.util.ClassUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author ytrue
 * @date 2022/9/30 09:23
 * @description ClassPathResource
 */
public class ClassPathResource implements Resource {

    /**
     * 路径
     */
    private final String path;

    /**
     * 类加载器
     */
    private ClassLoader classLoader;

    public ClassPathResource(String path) {
        this(path, null);
    }

    public ClassPathResource(String path, ClassLoader classLoader) {
        Assert.notNull(path, "Path must not be null");
        this.path = path;
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream is = classLoader.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException(
                    this.path + " cannot be opened because it does not exist");
        }
        return is;
    }
}
