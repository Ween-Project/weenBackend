package com.ween.mapper;

import com.ween.dto.response.OrganizationResponse;
import com.ween.entity.Organization;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    OrganizationResponse toOrganizationResponse(Organization organization);}