package com.ytrue.ioc.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ytrue
 * @date 2022/9/30 09:22
 * @description 资源加载接口
 */
public interface Resource {

    /**
     * 获取输入流
     *
     * @return
     * @throws IOException
     */
    InputStream getInputStream() throws IOException;
}
