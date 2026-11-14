package com.transport.assignment.dto.response;

import java.util.List;

public record AssignmentResponse(

        String status,

        Long totalCost,

        List<LaneAssignmentResponse> assignments,

        List<Long> selectedTransporters

) {
}
