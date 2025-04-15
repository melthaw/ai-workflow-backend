package com.fastgpt.ai.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Implementation of MultipartFile that wraps an InputStream
 */
public class MultipartFileFromInputStream implements MultipartFile {
    private final InputStream inputStream;
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private byte[] cachedBytes;

    public MultipartFileFromInputStream(InputStream inputStream, String originalFilename, String contentType) {
        this.inputStream = inputStream;
        this.name = originalFilename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        try {
            return inputStream.available() == 0;
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public long getSize() {
        try {
            return inputStream.available();
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public byte[] getBytes() throws IOException {
        if (cachedBytes == null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            cachedBytes = outputStream.toByteArray();
        }
        return cachedBytes;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo(File) is not supported, use transferTo(Path) instead");
    }

    @Override
    public void transferTo(Path dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException("transferTo(Path) not supported for InputStream-based MultipartFile");
    }
} 