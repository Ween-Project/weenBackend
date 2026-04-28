package com.ween.mapper;

import com.ween.dto.response.CertificateResponse;
import com.ween.entity.Certificate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CertificateMapper {
    @Mapping(target = "eventTitle", ignore = true)
    CertificateResponse toCertificateResponse(Certificate certificate);

    Certificate toCertificate(CertificateResponse certificateResponse);
}
