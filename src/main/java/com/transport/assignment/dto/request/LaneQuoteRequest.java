package com.transport.assignment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LaneQuoteRequest(

        @NotNull
        @Positive
        Long laneId,

        @NotNull
        @Positive
        Long quote

) {
}
