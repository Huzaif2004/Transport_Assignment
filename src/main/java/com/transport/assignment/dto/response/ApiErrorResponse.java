package com.transport.assignment.dto.response;

import java.util.List;

public record ApiErrorResponse(
        String status,
        String message,
        List<String> errors
) {
}
