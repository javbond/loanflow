package com.loanflow.document.service.impl;

import com.loanflow.document.service.StorageService;
import com.loanflow.util.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO implementation of StorageService
 */
@Service
@Slf4j
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;
    private final String defaultBucket;

    public MinioStorageService(
            @Value("${minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${minio.access-key:minioadmin}") String accessKey,
            @Value("${minio.secret-key:minioadmin}") String secretKey,
            @Value("${minio.bucket:loanflow-documents}") String defaultBucket) {

        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.defaultBucket = defaultBucket;

        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(defaultBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
                log.info("Created bucket: {}", defaultBucket);
            }
        } catch (Exception e) {
            log.warn("Could not ensure bucket exists: {}", e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String objectKey) {
        try (InputStream inputStream = file.getInputStream()) {
            return upload(inputStream, objectKey, file.getContentType(), file.getSize());
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage());
            throw new StorageException("Failed to upload file to storage", e);
        }
    }

    @Override
    public String upload(InputStream inputStream, String objectKey, String contentType, long size) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Uploaded object: {}/{}", defaultBucket, objectKey);
            return defaultBucket + "/" + objectKey;
        } catch (Exception e) {
            log.error("Failed to upload stream: {}", e.getMessage());
            throw new StorageException("Failed to upload to storage", e);
        }
    }

    @Override
    public String generatePresignedUrl(String bucket, String objectKey, int expirationMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirationMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL: {}", e.getMessage());
            throw new StorageException("Failed to generate download URL", e);
        }
    }

    @Override
    public void delete(String bucket, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted object: {}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.error("Failed to delete object: {}", e.getMessage());
            throw new StorageException("Failed to delete from storage", e);
        }
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InputStream download(String bucket, String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download object: {}", e.getMessage());
            throw new StorageException("Failed to download from storage", e);
        }
    }
}
