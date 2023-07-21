package com.ytrue.ioc.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * @author ytrue
 * @date 2022/9/30 09:24
 * @description FileSystemResource
 */
public class FileSystemResource implements Resource {

    /**
     * 文件
     */
    private final File file;

    /**
     * 路径
     */
    private final String path;

    public FileSystemResource(File file) {
        this.file = file;
        this.path = file.getPath();
    }

    public FileSystemResource(String path) {
        this.file = new File(path);
        this.path = path;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Files.newInputStream(this.file.toPath());
    }

    public final String getPath() {
        return this.path;
    }
}

