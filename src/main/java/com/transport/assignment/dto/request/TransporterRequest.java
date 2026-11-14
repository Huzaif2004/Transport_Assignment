package com.transport.assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record TransporterRequest(

        @NotNull
        @Positive
        Long id,

        @NotBlank
        String name,

        @NotNull
        List<@Valid LaneQuoteRequest> laneQuotes

) {
}
