package com.ween.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${ween.cloudinary.environment:dev}")
    private String environment;

    private String getEnv() {
        return (environment != null && !environment.isBlank()) ? environment.trim() : "dev";
    }

    private String resolvePublicId(String publicId) {
        String env = getEnv();
        String prefix = "ween/" + env + "/";
        if (publicId.startsWith("ween/")) {
            return publicId;
        }
        return prefix + publicId;
    }

    public String uploadPdf(byte[] pdfBytes, String publicId) throws IOException {
        String finalPublicId = resolvePublicId(publicId);
        Map<?, ?> params = ObjectUtils.asMap(
                "public_id", finalPublicId,
                "resource_type", "image",
                "format", "pdf",
                "overwrite", true
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, params);
        return (String) uploadResult.get("secure_url");
    }

    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        String folderPath = "ween/" + getEnv() + "/" + subFolder;
        Map<?, ?> params = ObjectUtils.asMap(
                "folder", folderPath,
                "resource_type", "auto"
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return (String) uploadResult.get("secure_url");
    }
}
