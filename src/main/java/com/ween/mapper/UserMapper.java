package com.ween.mapper;

import com.ween.dto.response.PublicProfileResponse;
import com.ween.dto.response.UserResponse;
import com.ween.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    PublicProfileResponse toPublicProfileResponse(User user);}