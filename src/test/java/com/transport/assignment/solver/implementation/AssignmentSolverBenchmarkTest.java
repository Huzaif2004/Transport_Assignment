
package com.transport.assignment.solver.implementation;

import com.transport.assignment.solver.AssignmentResult;
import com.transport.assignment.solver.QuoteMatrix;

import org.junit.jupiter.api.Test;

import java.util.*;

class AssignmentSolverBenchmarkTest {

    @Test
    void compareSolvers() {

        // Same input for both algorithms
        QuoteMatrix matrix = randomDense(20, 15, 1234L);

        int maxTransporters = 5;

        BruteForceAssignmentSolver bruteForce =
                new BruteForceAssignmentSolver();

        BranchAndBoundAssignmentSolver branchAndBound =
                new BranchAndBoundAssignmentSolver();

        // Warm-up
        for (int i = 0; i < 5; i++) {
            bruteForce.solve(matrix, maxTransporters);
            branchAndBound.solve(matrix, maxTransporters);
        }

        List<Long> bruteTimes = new ArrayList<>();
        List<Long> branchTimes = new ArrayList<>();

        // Measure 15 executions
        for (int i = 0; i < 15; i++) {

            long start = System.nanoTime();

            AssignmentResult bruteResult =
                    bruteForce.solve(matrix, maxTransporters);

            long bruteTime = System.nanoTime() - start;

            start = System.nanoTime();

            AssignmentResult branchResult =
                    branchAndBound.solve(matrix, maxTransporters);

            long branchTime = System.nanoTime() - start;
            
            // Verify correctness
            if (!bruteResult.totalCost().equals(branchResult.totalCost())) {

                System.out.println("Lane IDs: " + matrix.laneIds());
    System.out.println("Transporter IDs: " + matrix.transporterIds());

    System.out.println("Quote Matrix:");
    System.out.println(Arrays.deepToString(matrix.values()));
                throw new AssertionError("Optimal costs do not match!");
            }

            bruteTimes.add(bruteTime);
            branchTimes.add(branchTime);
        }

        double bruteMedian = median(bruteTimes) / 1_000_000.0;
        double branchMedian = median(branchTimes) / 1_000_000.0;

        System.out.println("\n========== BENCHMARK RESULTS ==========");

        System.out.printf("Brute Force       : %.3f ms%n", bruteMedian);
        System.out.printf("Branch and Bound  : %.3f ms%n", branchMedian);

        System.out.printf(
                "Speedup           : %.2fx%n",
                bruteMedian / branchMedian
        );

        System.out.println("=======================================");
    }

    private static long median(List<Long> times) {

        Collections.sort(times);

        return times.get(times.size() / 2);
    }

    private static QuoteMatrix randomDense(
            int transporters, int lanes, long seed) {

        Random random = new Random(seed);

        List<Long> laneIds = new ArrayList<>();
        List<Long> transporterIds = new ArrayList<>();

        long[][] values = new long[transporters][lanes];

        for (int l = 0; l < lanes; l++) {
            laneIds.add((long) l + 1);
        }

        for (int t = 0; t < transporters; t++) {

            transporterIds.add((long) t + 1);

            for (int l = 0; l < lanes; l++) {
                values[t][l] = 1000 + random.nextInt(9000);
            }
        }

        return new QuoteMatrix(laneIds, transporterIds, values);
    }
}
