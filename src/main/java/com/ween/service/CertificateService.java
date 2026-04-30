package com.ween.service;

import com.ween.config.ThymeleafConfig;
import com.ween.entity.Certificate;
import com.ween.entity.Event;
import com.ween.entity.User;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CertificateRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ThymeleafConfig thymeleafConfig;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public byte[] createCertificatePdf(String certificateId) throws Exception {

        // Fetch the certificate
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found. ID: " + certificateId));

        // Fetch user and event details
        User user = userRepository.findById(certificate.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found. ID: " + certificate.getUserId()));

        Event event = eventRepository.findById(certificate.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found. ID: " + certificate.getEventId()));

        // Setup Thymeleaf context
        Context context = new Context();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String formattedDate = certificate.getIssuedAt() != null
                ? certificate.getIssuedAt().format(formatter)
                : "Date not specified";

        context.setVariable("userName", user.getFullName());
        context.setVariable("title", event.getTitle());
        context.setVariable("certificateNumber", certificate.getCertificateNumber());
        context.setVariable("issueDate", formattedDate);

        // Use category name for the template dynamically
        String categoryTemplate = event.getCategory().name().toLowerCase();
        context.setVariable("templateType", categoryTemplate);

        // Process HTML to String
        String htmlContent;
        try {
            htmlContent = thymeleafConfig.customTemplateEngine().process(categoryTemplate, context);
        } catch (Exception e) {
            // Log warning if specific template is missing and fallback to default
            log.warn("Specific template not found: {}. Falling back to default template.", categoryTemplate);
            htmlContent = thymeleafConfig.customTemplateEngine().process("default_certificate", context);
        }

        // Convert HTML to PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        }
    }

    public Certificate getCertificateById(String certificateId) {
        return certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateId));
    }

    public Certificate getCertificateByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + certificateNumber));
    }

    public List<Certificate> getUserCertificates(String userId) {
        return certificateRepository.findByUserId(userId);
    }


    @Transactional
    public void deleteCertificate(String certificateId) {
        Certificate certificate = getCertificateById(certificateId);

        try {
            // StorageService removed - PDF deletion functionality disabled
            log.info("Certificate PDF deletion skipped: {}", certificateId);
        } catch (Exception e) {
            log.warn("Failed to delete PDF file", e);
        }

        certificateRepository.delete(certificate);
        log.info("Certificate deleted: {}", certificateId);
    }

    public boolean verifyCertificate(String certificateNumber) {
        return certificateRepository.findByCertificateNumber(certificateNumber).isPresent();
    }

    public Integer getUserCertificateCount(String userId) {
        return (int) getUserCertificates(userId).size();
    }


    public byte[] downloadCertificatePdf(String userId, String id) {
        return new byte[0];
    }

    public String generateCertificatesAsync(String userId, String eventId) {
        return userId;
    }
}