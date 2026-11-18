package com.spa.spabookingservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spa.spabookingservice.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryStorageServiceImpl implements FileStorageService {

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp", "image/jpg"
    );

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Chỉ chấp nhận định dạng ảnh JPG, PNG hoặc WEBP!");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "lotus-spa/" + folder,
                            "resource_type", "image"
                    )
            );

            String secureUrl = uploadResult.get("secure_url").toString();
            log.info("Upload ảnh thành công lên Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Lỗi khi upload ảnh lên Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Không thể tải ảnh lên hệ thống lưu trữ đám mây", e);
        }
    }
}