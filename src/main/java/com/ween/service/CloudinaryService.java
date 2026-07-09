package com.ween.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadPdf(byte[] pdfBytes, String publicId) throws IOException {
        Map<?, ?> params = ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "image",
                "format", "pdf",
                "overwrite", true
        );
        Map<?, ?> uploadResult = cloudinary.uploader().upload(pdfBytes, params);
        return (String) uploadResult.get("secure_url");
    }
}
