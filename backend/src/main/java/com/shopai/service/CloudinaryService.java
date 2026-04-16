package com.shopai.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shopai.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    /**
     * Uploads a single image to Cloudinary under the "shopai/products" folder.
     *
     * @param file the multipart file from the HTTP request
     * @return the secure HTTPS URL of the uploaded image
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) {
        validateFile(file);

        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "shopai/products",
                    "resource_type", "image",
                    "quality", "auto:good",
                    "fetch_format", "auto"
            ));
            String secureUrl = (String) result.get("secure_url");
            log.info("Image uploaded to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Cloudinary upload failed: {}", e.getMessage());
            throw new BadRequestException("Cloudinary yükleme hatası: " + e.getMessage());
        }
    }

    /**
     * Deletes an image from Cloudinary by extracting its public_id from the URL.
     */
    @SuppressWarnings("unchecked")
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted from Cloudinary: {}", publicId);
            }
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for URL {}: {}", imageUrl, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Dosya boş olamaz");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("Dosya boyutu 10 MB'den büyük olamaz");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Geçersiz dosya türü. İzin verilenler: JPEG, PNG, WebP, GIF");
        }
    }

    /**
     * Extracts the Cloudinary public_id from a full URL.
     * Example URL: https://res.cloudinary.com/dx1obqyrc/image/upload/v123/shopai/products/abc.jpg
     * Returns: shopai/products/abc
     */
    private String extractPublicId(String url) {
        if (url == null || !url.contains("cloudinary.com")) return null;
        try {
            // Splits by "/upload/" and takes everything after the version number
            String afterUpload = url.split("/upload/")[1];
            // Remove version prefix like "v1234567890/"
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
            // Remove file extension
            int lastDot = withoutVersion.lastIndexOf('.');
            return lastDot > 0 ? withoutVersion.substring(0, lastDot) : withoutVersion;
        } catch (Exception e) {
            log.warn("Could not extract public_id from URL: {}", url);
            return null;
        }
    }
}
