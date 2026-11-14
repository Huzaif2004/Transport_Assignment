package com.transport.assignment.solver;

import java.util.List;

public record QuoteMatrix(
        List<Long> laneIds,
        List<Long> transporterIds,
        long[][] values
) {
}
