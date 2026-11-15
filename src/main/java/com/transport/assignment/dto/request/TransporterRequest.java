package com.transport.assignment.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransporterRequest(

                @NotNull @Positive Long id,

                @NotBlank String name,

                @NotEmpty List<@Valid LaneQuoteRequest> laneQuotes

) {
}
