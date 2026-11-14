package com.transport.assignment.solver.implementation;

import com.transport.assignment.exception.NoFeasibleAssignmentException;
import com.transport.assignment.solver.AssignmentResult;
import com.transport.assignment.solver.AssignmentSolver;
import com.transport.assignment.solver.LaneAssignment;
import com.transport.assignment.solver.QuoteMatrix;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Component
public class BruteForceAssignmentSolver implements AssignmentSolver {

    @Override
    public AssignmentResult solve(
            QuoteMatrix matrix,
            int maxTransporters
    ) {

        // Step 1: Basic validation
        if (matrix == null || maxTransporters < 1) {
            throw new IllegalArgumentException(
                    "Invalid matrix or maxTransporters value."
            );
        }

        int transporterCount = matrix.transporterIds().size();
        int laneCount = matrix.laneIds().size();

        if (transporterCount == 0 || laneCount == 0) {
            throw new NoFeasibleAssignmentException(
                    "No transporters or lanes available."
            );
        }

        validateMatrix(matrix);

        // Step 2: Determine how many candidate transporters to consider
        int k = Math.min(maxTransporters, transporterCount);

        // Holds the best solution found so far
        SearchState state = new SearchState();

        // Stores the indices of the current transporter combination
        int[] selected = new int[k];

        // Step 3: Generate all combinations and evaluate them
        generateCombinations(
                matrix,
                k,
                0,
                0,
                selected,
                state
        );

        // Step 4: Check whether a feasible solution was found
        if (state.bestResult == null) {
            throw new NoFeasibleAssignmentException(
                    "No feasible assignment found for the given transporter limit."
            );
        }

        return state.bestResult;
    }


    private void generateCombinations(
            QuoteMatrix matrix,
            int k,
            int start,
            int depth,
            int[] selected,
            SearchState state
    ) {

        int transporterCount = matrix.transporterIds().size();

        // We have selected k transporters.
        // Evaluate this combination.
        if (depth == k) {

            AssignmentResult candidate =
                    evaluateCombination(matrix, selected);

            if (candidate != null &&
                    (state.bestResult == null ||
                            candidate.totalCost() < state.bestResult.totalCost())) {

                state.bestResult = candidate;
            }

            return;
        }

        // Generate combinations without repetition.
        // Leave enough remaining indices to complete the combination.
        for (int i = start;
             i <= transporterCount - (k - depth);
             i++) {

            selected[depth] = i;

            generateCombinations(
                    matrix,
                    k,
                    i + 1,
                    depth + 1,
                    selected,
                    state
            );
        }
    }


    private AssignmentResult evaluateCombination(
            QuoteMatrix matrix,
            int[] selected
    ) {

        long totalCost = 0;

        List<LaneAssignment> assignments = new ArrayList<>();

        // TreeSet keeps transporter IDs unique and sorted.
        TreeSet<Long> usedTransporters = new TreeSet<>();

        int laneCount = matrix.laneIds().size();

        // Assign every lane to its cheapest available transporter.
        for (int laneIndex = 0;
             laneIndex < laneCount;
             laneIndex++) {

            long cheapestQuote = Long.MAX_VALUE;
            int cheapestTransporterIndex = -1;

            // Check the selected transporters for this lane.
            for (int transporterIndex : selected) {

                long quote =
                        matrix.values()[transporterIndex][laneIndex];

                // -1 means this transporter did not quote for this lane.
                if (quote == -1) {
                    continue;
                }

                if (quote < cheapestQuote) {

                    cheapestQuote = quote;
                    cheapestTransporterIndex = transporterIndex;
                }

                // If prices are equal, prefer the lower transporter ID.
                else if (quote == cheapestQuote &&
                        matrix.transporterIds().get(transporterIndex)
                                < matrix.transporterIds().get(cheapestTransporterIndex)) {

                    cheapestTransporterIndex = transporterIndex;
                }
            }

            // None of the selected transporters quoted for this lane.
            // Therefore, this combination cannot cover all lanes.
            if (cheapestTransporterIndex == -1) {
                return null;
            }

            // Add the cheapest quote to the total.
            totalCost = Math.addExact(totalCost, cheapestQuote);

            Long laneId = matrix.laneIds().get(laneIndex);

            Long transporterId =
                    matrix.transporterIds().get(cheapestTransporterIndex);

            assignments.add(
                    new LaneAssignment(laneId, transporterId)
            );

            usedTransporters.add(transporterId);
        }

        return new AssignmentResult(
                totalCost,
                assignments,
                new ArrayList<>(usedTransporters)
        );
    }


    private void validateMatrix(QuoteMatrix matrix) {

        int transporterCount = matrix.transporterIds().size();
        int laneCount = matrix.laneIds().size();

        long[][] values = matrix.values();

        if (values == null || values.length != transporterCount) {
            throw new IllegalArgumentException(
                    "Quote matrix row count does not match transporter count."
            );
        }

        for (long[] row : values) {

            if (row == null || row.length != laneCount) {
                throw new IllegalArgumentException(
                        "Quote matrix column count does not match lane count."
                );
            }
        }
    }


    // Holds the best solution for one solve() execution.
    // It is local to the request, so the Spring singleton solver
    // does not share mutable search state between requests.
    private static class SearchState {

        private AssignmentResult bestResult;
    }
}