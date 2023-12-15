package com.ytrue.web.multipart;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author ytrue
 * @date 2023-12-15 13:51
 * @description MultipartFile
 */
public interface MultipartFile extends InputStreamSource {

    /**
     * 获取名称
     *
     * @return
     */
    String getName();

    /**
     * 获取文件名
     *
     * @return
     */
    String getOriginalFilename();

    /**
     * 获取类型
     *
     * @return
     */
    String getContentType();

    /**
     * 是否为空
     *
     * @return
     */
    boolean isEmpty();

    /**
     * 大小
     *
     * @return
     */
    long getSize();

    /**
     * 大小
     *
     * @return
     * @throws IOException
     */
    byte[] getBytes() throws IOException;

    /**
     * 获取流
     *
     * @return
     * @throws IOException
     */
    @Override
    InputStream getInputStream() throws IOException;

    /**
     * 转换
     *
     * @param dest
     * @throws IOException
     * @throws IllegalStateException
     */
    void transferTo(File dest) throws IOException, IllegalStateException;

    default void transferTo(Path dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(getInputStream(), Files.newOutputStream(dest));
    }

}
