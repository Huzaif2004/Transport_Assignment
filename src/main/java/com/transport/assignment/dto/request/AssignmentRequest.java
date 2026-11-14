package com.transport.assignment.dto.request;

import jakarta.validation.constraints.Min;

public record AssignmentRequest(

        @Min(1)
        int maxTransporters

) {
}
