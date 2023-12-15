package com.ytrue.web.multipart;

import org.springframework.util.FileCopyUtils;

import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author ytrue
 * @date 2023-12-15 13:52
 * @description StandardMultipartFile
 */
public class StandardMultipartFile implements MultipartFile, Serializable {

    private final Part part;

    private final String filename;

    public StandardMultipartFile(Part part, String filename) {
        this.part = part;
        this.filename = filename;
    }

    @Override
    public String getName() {
        return this.part.getName();
    }

    @Override
    public String getOriginalFilename() {
        return this.filename;
    }

    @Override
    public String getContentType() {
        return this.part.getContentType();
    }

    @Override
    public boolean isEmpty() {
        return (this.part.getSize() == 0);
    }

    @Override
    public long getSize() {
        return this.part.getSize();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return FileCopyUtils.copyToByteArray(this.part.getInputStream());
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.part.getInputStream();
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        this.part.write(dest.getPath());
        if (dest.isAbsolute() && !dest.exists()) {
            FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest.toPath()));
        }
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest));
    }

}
