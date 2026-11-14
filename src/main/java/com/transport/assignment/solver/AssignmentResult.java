package com.transport.assignment.solver;

import java.util.List;

public record AssignmentResult(
        Long totalCost,
        List<LaneAssignment> assignments,
        List<Long> selectedTransporters
) {
}
