package com.transport.assignment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignmentRequest(

                @Min(1) @NotNull Integer maxTransporters

) {
}
