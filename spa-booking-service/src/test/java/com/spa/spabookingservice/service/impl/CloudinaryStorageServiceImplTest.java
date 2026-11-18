package com.spa.spabookingservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryStorageServiceImplTest {

    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;

    @InjectMocks
    private CloudinaryStorageServiceImpl storageService;

    @Test
    @DisplayName("1. Upload ảnh hợp lệ (PNG) lên Cloudinary thành công")
    void uploadImage_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "spa.png", "image/png", "dummy-bytes".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/spa.png"));

        String url = storageService.uploadImage(file, "services");

        assertEquals("https://res.cloudinary.com/demo/image/upload/spa.png", url);
    }

    @Test
    @DisplayName("2. Từ chối file không phải định dạng ảnh (PDF/EXE)")
    void uploadImage_FailWhenInvalidContentType() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", "data".getBytes());

        assertThrows(IllegalArgumentException.class, () -> storageService.uploadImage(pdfFile, "services"));
        verifyNoInteractions(cloudinary);
    }
}