package com.ween.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudinaryServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);

        cloudinaryService = new CloudinaryService(cloudinary);
        ReflectionTestUtils.setField(cloudinaryService, "environment", "test");
    }

    @Test
    void uploadPdfReturnsSecureUrl() throws IOException {
        byte[] pdfBytes = "pdf-content".getBytes();
        String publicId = "cert-123";

        Map<String, Object> mockResponse = Map.of("secure_url", "https://res.cloudinary.com/test/cert-123.pdf");
        when(uploader.upload(eq(pdfBytes), any(Map.class))).thenReturn(mockResponse);

        String secureUrl = cloudinaryService.uploadPdf(pdfBytes, publicId);

        assertThat(secureUrl).isEqualTo("https://res.cloudinary.com/test/cert-123.pdf");
    }

    @Test
    void uploadFileReturnsSecureUrl() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        byte[] fileBytes = "image-content".getBytes();
        when(file.getBytes()).thenReturn(fileBytes);

        Map<String, Object> mockResponse = Map.of("secure_url", "https://res.cloudinary.com/test/image.jpg");
        when(uploader.upload(eq(fileBytes), any(Map.class))).thenReturn(mockResponse);

        String secureUrl = cloudinaryService.uploadFile(file, "posts/media");

        assertThat(secureUrl).isEqualTo("https://res.cloudinary.com/test/image.jpg");
    }

    @Test
    void uploadPostMediaDelegatesToUploadFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        byte[] fileBytes = "media-content".getBytes();
        when(file.getBytes()).thenReturn(fileBytes);

        Map<String, Object> mockResponse = Map.of("secure_url", "https://res.cloudinary.com/test/post.mp4");
        when(uploader.upload(eq(fileBytes), any(Map.class))).thenReturn(mockResponse);

        String secureUrl = cloudinaryService.uploadPostMedia(file);

        assertThat(secureUrl).isEqualTo("https://res.cloudinary.com/test/post.mp4");
    }
}
