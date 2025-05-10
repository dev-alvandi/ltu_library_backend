package com.noahalvandi.dbbserver.service;

import com.noahalvandi.dbbserver.dto.HasImageUrl;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Service {

    String uploadFile(String key, MultipartFile file) throws IOException;

    void deleteFile(String key);

    String getFileUrl(String key);

    public String generatePresignedUrl(String key, int expirationInMinutes);

    public void uploadBarcodeImage(String key, byte[] imageBytes);

    public  <T extends HasImageUrl> void injectS3ImageUrlIntoDto(Page<T> page);
}