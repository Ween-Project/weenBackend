package com.ween.service;

import com.ween.entity.Certificate;
import com.ween.enums.EventCategory;
import com.ween.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateTriggerService {

    private final CertificateRepository certificateRepository;

    public void autoGenerateCertificateRecord(String userId, String eventId, EventCategory category) {
        Certificate cert = new Certificate();
        cert.setUserId(userId);
        cert.setEventId(eventId);
        cert.setTemplateType(category);

        String uniqueNo = "CERT-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        cert.setCertificateNumber(uniqueNo);

        cert.setIssuedAt(LocalDateTime.now());


        certificateRepository.save(cert);
    }
}
