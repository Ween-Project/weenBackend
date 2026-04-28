package com.ween.mapper;

import com.ween.dto.response.NotificationResponse;
import com.ween.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toNotificationResponse(Notification notification);}