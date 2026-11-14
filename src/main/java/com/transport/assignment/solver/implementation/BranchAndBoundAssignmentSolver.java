package com.transport.assignment.solver.implementation;

import com.transport.assignment.exception.NoFeasibleAssignmentException;
import com.transport.assignment.solver.AssignmentResult;
import com.transport.assignment.solver.AssignmentSolver;
import com.transport.assignment.solver.LaneAssignment;
import com.transport.assignment.solver.QuoteMatrix;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Primary
public class BranchAndBoundAssignmentSolver implements AssignmentSolver {

    private QuoteMatrix matrix;

    private int laneCount;
    private int transporterCount;
    private int k;

    private int[] selected;
    private int[] bestAssignments;

    private long bestCost;

    private long[][] values;
    private int[] transporterOrder;

    @Override
    public synchronized AssignmentResult solve(
            QuoteMatrix matrix, int maxTransporters) {

        if (matrix == null || maxTransporters <= 0) {
            throw new IllegalArgumentException("Invalid input");
        }

        this.matrix = matrix;
        this.values = matrix.values();

        laneCount = matrix.laneIds().size();
        transporterCount = matrix.transporterIds().size();

        if (laneCount == 0 || transporterCount == 0) {
            throw new NoFeasibleAssignmentException("Empty input");
        }

        if (values.length != transporterCount) {
            throw new IllegalArgumentException("Invalid matrix dimensions");
        }

        for (long[] row : values) {
            if (row == null || row.length != laneCount) {
                throw new IllegalArgumentException("Invalid matrix dimensions");
            }

            for (long cost : row) {
                if (cost < -1) {
                    throw new IllegalArgumentException("Invalid quote");
                }
            }
        }

        k = Math.min(maxTransporters, transporterCount);

        selected = new int[k];
        bestAssignments = null;
        bestCost = Long.MAX_VALUE;

        transporterOrder = new int[transporterCount];

        Integer[] order = new Integer[transporterCount];

        for (int i = 0; i < transporterCount; i++) {
            order[i] = i;
        }

        Arrays.sort(order, Comparator.comparing(
                i -> matrix.transporterIds().get(i)));

        for (int i = 0; i < transporterCount; i++) {
            transporterOrder[i] = order[i];
        }

        search(0, 0);

        if (bestAssignments == null) {
            throw new NoFeasibleAssignmentException(
                    "No feasible assignment found");
        }

        return buildResult();
    }

    private void search(int start, int selectedCount) {

        if (selectedCount == k) {
            evaluate();
            return;
        }

        long lowerBound = calculateLowerBound(start, selectedCount);

        // Stop if some lane can't be covered (MAX_VALUE) or the best case can't beat the best plan.
        if (lowerBound == Long.MAX_VALUE || lowerBound > bestCost) {
            return;
        }

        int remaining = k - selectedCount;

        for (int i = start; i <= transporterCount - remaining; i++) {

            selected[selectedCount] = transporterOrder[i];

            search(i + 1, selectedCount + 1);
        }
    }

    private long calculateLowerBound(int start, int selectedCount) {

        long lowerBound = 0;

        for (int lane = 0; lane < laneCount; lane++) {

            long minimum = Long.MAX_VALUE;

            for (int i = 0; i < selectedCount; i++) {

                long cost = values[selected[i]][lane];

                if (cost >= 0) {
                    minimum = Math.min(minimum, cost);
                }
            }

            for (int i = start; i < transporterCount; i++) {

                int transporter = transporterOrder[i];

                long cost = values[transporter][lane];

                if (cost >= 0) {
                    minimum = Math.min(minimum, cost);
                }
            }

            if (minimum == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }

            lowerBound = Math.addExact(lowerBound, minimum);
        }

        return lowerBound;
    }

    private void evaluate() {

        long totalCost = 0;
        int[] assignments = new int[laneCount];

        for (int lane = 0; lane < laneCount; lane++) {

            long minimum = Long.MAX_VALUE;
            int bestTransporter = -1;

            for (int i = 0; i < k; i++) {

                int transporter = selected[i];

                long cost = values[transporter][lane];

                if (cost == -1) {
                    continue;
                }

                if (cost < minimum ||
                        (cost == minimum &&
                         matrix.transporterIds().get(transporter) <
                         matrix.transporterIds().get(bestTransporter))) {

                    minimum = cost;
                    bestTransporter = transporter;
                }
            }

            if (bestTransporter == -1) {
                return;
            }

            assignments[lane] = bestTransporter;
            totalCost = Math.addExact(totalCost, minimum);
        }

        if (totalCost < bestCost ||
                (totalCost == bestCost && isBetter(assignments))) {

            bestCost = totalCost;
            bestAssignments = assignments;
        }
    }

    private boolean isBetter(int[] assignments) {

        if (bestAssignments == null) {
            return true;
        }

        for (int lane = 0; lane < laneCount; lane++) {

            long currentId = matrix.transporterIds()
                    .get(assignments[lane]);

            long previousId = matrix.transporterIds()
                    .get(bestAssignments[lane]);

            if (currentId < previousId) {
                return true;
            }

            if (currentId > previousId) {
                return false;
            }
        }

        return false;
    }

    private AssignmentResult buildResult() {

        List<LaneAssignment> assignments = new ArrayList<>();
        Set<Long> usedTransporters = new TreeSet<>();

        for (int lane = 0; lane < laneCount; lane++) {

            int transporter = bestAssignments[lane];

            assignments.add(new LaneAssignment(
                    matrix.laneIds().get(lane),
                    matrix.transporterIds().get(transporter)
            ));

            usedTransporters.add(
                    matrix.transporterIds().get(transporter));
        }

        return new AssignmentResult(
                bestCost,
                assignments,
                new ArrayList<>(usedTransporters)
        );
    }
}