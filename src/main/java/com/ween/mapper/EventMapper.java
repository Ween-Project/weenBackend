package com.ween.mapper;

import com.ween.dto.response.EventDetailResponse;
import com.ween.dto.response.EventResponse;
import com.ween.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "currentRegistrations", ignore = true)
    EventResponse toEventResponse(Event event);
    
    @Mapping(target = "currentRegistrations", ignore = true)
    @Mapping(target = "attendeeCount", ignore = true)
    @Mapping(target = "organizationName", ignore = true)
    @Mapping(target = "userRegistered", ignore = true)
    @Mapping(target = "userAttended", ignore = true)
    EventDetailResponse toEventDetailResponse(Event event);
}
