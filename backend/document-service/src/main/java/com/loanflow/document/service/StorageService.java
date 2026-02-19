package com.loanflow.document.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Interface for document storage operations (MinIO/S3)
 */
public interface StorageService {

    /**
     * Upload file to storage
     *
     * @param file      MultipartFile to upload
     * @param objectKey Storage key/path
     * @return Full storage path (bucket/key)
     */
    String upload(MultipartFile file, String objectKey);

    /**
     * Upload from input stream
     *
     * @param inputStream File content stream
     * @param objectKey   Storage key/path
     * @param contentType MIME type
     * @param size        File size
     * @return Full storage path
     */
    String upload(InputStream inputStream, String objectKey, String contentType, long size);

    /**
     * Generate presigned download URL
     *
     * @param bucket            Storage bucket
     * @param objectKey         Object key
     * @param expirationMinutes URL expiration in minutes
     * @return Presigned URL for download
     */
    String generatePresignedUrl(String bucket, String objectKey, int expirationMinutes);

    /**
     * Delete object from storage
     *
     * @param bucket    Storage bucket
     * @param objectKey Object key
     */
    void delete(String bucket, String objectKey);

    /**
     * Check if object exists
     *
     * @param bucket    Storage bucket
     * @param objectKey Object key
     * @return true if exists
     */
    boolean exists(String bucket, String objectKey);

    /**
     * Get object as input stream
     *
     * @param bucket    Storage bucket
     * @param objectKey Object key
     * @return InputStream of object
     */
    InputStream download(String bucket, String objectKey);
}
