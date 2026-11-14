package com.transport.assignment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LaneRequest(

        @NotNull
        @Positive
        Long id,

        @NotBlank
        String origin,

        @NotBlank
        String destination

) {
}
