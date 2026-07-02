package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupRoomRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private String photoUrl;
}
