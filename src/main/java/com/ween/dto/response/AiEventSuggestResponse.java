package com.ween.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEventSuggestResponse {
    private String description;

    @JsonDeserialize(using = StringOrListDeserializer.class)
    private List<String> requirements;

    @JsonDeserialize(using = StringOrListDeserializer.class)
    private List<String> schedule;
}
