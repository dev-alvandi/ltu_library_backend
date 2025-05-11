package com.noahalvandi.dbbserver.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.noahalvandi.dbbserver.dto.HasImageUrl;
import com.noahalvandi.dbbserver.util.BarcodeUtil;
import com.noahalvandi.dbbserver.util.GlobalConstants;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImplementation implements S3Service {

    Dotenv dotenv = Dotenv.configure().load();
    private final String BUCKET_NAME = dotenv.get("AWS_BUCKET_NAME");

    private final AmazonS3 amazonS3;

    @Override
    public String uploadFile(String key, MultipartFile file) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        amazonS3.putObject(BUCKET_NAME, key, file.getInputStream(), metadata);
        return amazonS3.getUrl(BUCKET_NAME, key).toString();
    }

    @Override
    public void deleteFile(String key) {
        amazonS3.deleteObject(BUCKET_NAME, key);
    }

    @Override
    public String getFileUrl(String key) {
        return amazonS3.getUrl(BUCKET_NAME, key).toString();
    }

    public String generatePresignedUrl(String key, int expirationInMinutes) {
        Date expiration = new Date(System.currentTimeMillis() + expirationInMinutes * 60 * 1000L);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(BUCKET_NAME, key)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);
        return amazonS3.generatePresignedUrl(request).toString();
    }

    @Override
    public void uploadBarcodeImage(String key, byte[] imageBytes) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(imageBytes.length);
        metadata.setContentType("image/png");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
        amazonS3.putObject(BUCKET_NAME, key, inputStream, metadata);
    }

    @Override
    public  <T extends HasImageUrl> void injectS3ImageUrlIntoDto(Page<T> page) {
        page.getContent().forEach(item -> {
            if (item.getImageUrl() != null) {
                String presignedUrl = this.generatePresignedUrl(item.getImageUrl(), GlobalConstants.CLOUD_URL_EXPIRATION_TIME_IN_MINUTES);
                item.setImageUrl(presignedUrl);
            }
        });
    }

    @Override
    public String uploadResourceImage(String resourceName, UUID resourceId, MultipartFile file) throws IOException {
        String key = resourceName.toLowerCase() + "s/" + resourceId + "/" + file.getOriginalFilename();
        uploadFile(key, file);
        return key;
    }

    @Override
    public void uploadResourceBarcodeImage(String resourceName, String imageUrl, UUID copyId, String barcode) throws Exception {
        String imageId = imageUrl.split("/")[1];
        String key = String.format(
                "%ss/%s/barcodes/%s/%s.png",
                resourceName.toLowerCase(), imageId, copyId, barcode
        );

        byte[] barcodeImage = BarcodeUtil.generateBarcodePng(barcode);
        this.uploadBarcodeImage(key, barcodeImage);
    }
}
