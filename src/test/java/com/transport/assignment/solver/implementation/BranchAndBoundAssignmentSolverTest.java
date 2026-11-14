package com.transport.assignment.solver.implementation;

import com.transport.assignment.exception.NoFeasibleAssignmentException;
import com.transport.assignment.solver.AssignmentResult;
import com.transport.assignment.solver.LaneAssignment;
import com.transport.assignment.solver.QuoteMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BranchAndBoundAssignmentSolver}.
 *
 * <p>Expected values were verified independently by exhaustive search. They differ
 * from the sample responses in the assignment PDF, which are illustrative only and
 * violate the PDF's own rules (see the README, "Sample discrepancies").
 *
 * <p>Quote value -1 means "this transporter did not quote this lane".
 */
class BranchAndBoundAssignmentSolverTest {

    private static final long X = -1; // no quote

    private BranchAndBoundAssignmentSolver solver;

    @BeforeEach
    void setUp() {
        solver = new BranchAndBoundAssignmentSolver();
    }

    // ------------------------------------------------------------------ fixtures

    /** If your QuoteMatrix constructor has a different parameter order, change it here only. */
    static QuoteMatrix matrix(long[] laneIds, long[] transporterIds, long[][] values) {
        return new QuoteMatrix(boxed(laneIds), boxed(transporterIds), values);
    }

    /** If your AssignmentResult accessor names differ, change these two helpers only. */
    static List<Long> selected(AssignmentResult r) {
        return r.selectedTransporters();
    }

    static List<LaneAssignment> assignments(AssignmentResult r) {
        return r.assignments();
    }

    private static List<Long> boxed(long[] ids) {
        List<Long> list = new ArrayList<>();
        for (long id : ids) {
            list.add(id);
        }
        return list;
    }

    /** Test Case 2 from the PDF: X=1, Y=2, Z=3, W=4 over lanes 1..5. */
    static QuoteMatrix testCase2() {
        return matrix(
                new long[]{1, 2, 3, 4, 5},
                new long[]{1, 2, 3, 4},
                new long[][]{
                        {1000, 1500, X, X, X},
                        {X, X, 2000, 1800, X},
                        {X, X, X, X, 1200},
                        {1100, X, X, 1750, X}
                });
    }

    /** Test Case 1 from the PDF: 5 lanes, 7 transporters, everyone quoted every lane. */
    static QuoteMatrix testCase1() {
        return matrix(
                new long[]{1, 2, 3, 4, 5},
                new long[]{1, 2, 3, 4, 5, 6, 7},
                new long[][]{
                        {20835, 10512, 22105, 42481, 19862},
                        {48844, 31326, 18640, 45828, 18297},
                        {39020, 20648, 31438, 36447, 12789},
                        {14400, 44514, 14316, 10678, 13032},
                        {11601, 19760, 40870, 20635, 26421},
                        {35095, 12494, 17808, 36210, 39444},
                        {26070, 41098, 20932, 16897, 27938}
                });
    }

    /** First example in the PDF: 10 lanes, 7 transporters, each quoting only two lanes. */
    static QuoteMatrix specExample() {
        return matrix(
                new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10},
                new long[]{1, 2, 3, 4, 5, 6, 7},
                new long[][]{
                        {5000, 6000, X, X, X, X, X, X, X, X},   // T1
                        {X, X, 7000, 4000, X, X, X, X, X, X},   // T2
                        {X, X, X, X, 3000, 2000, X, X, X, X},   // T3
                        {X, X, X, X, X, X, 8000, 5000, X, X},   // T4
                        {X, X, X, X, X, X, X, X, 4500, 3500},   // T5
                        {5500, 6200, X, X, X, X, X, X, X, X},   // T6
                        {X, X, 7500, 4500, X, X, X, X, X, X}    // T7
                });
    }

    // ---------------------------------------------------------- Test Case 2 (PDF)

    @Test
    void testCase2_limitThree_usesOnlyTheForcedTransporters() {
        AssignmentResult r = solver.solve(testCase2(), 3);

        assertEquals(7500, r.totalCost());
        assertEquals(List.of(1L, 2L, 3L), selected(r));
        assertEquals(Map.of(1L, 1L, 2L, 1L, 3L, 2L, 4L, 2L, 5L, 3L), laneToTransporter(r));
    }

    @Test
    void testCase2_limitFour_usesWToSaveFifty() {
        AssignmentResult r = solver.solve(testCase2(), 4);

        assertEquals(7450, r.totalCost());
        assertEquals(List.of(1L, 2L, 3L, 4L), selected(r));
        assertEquals(4L, laneToTransporter(r).get(4L), "lane 4 should move to W (1750 < 1800)");
    }

    @Test
    void testCase2_limitLargerThanTransporterCount_isCapped() {
        AssignmentResult r = solver.solve(testCase2(), 10);

        assertEquals(7450, r.totalCost());
    }

    @Test
    void testCase2_limitTwo_isInfeasible() {
        assertThrows(NoFeasibleAssignmentException.class, () -> solver.solve(testCase2(), 2));
    }

    @Test
    void testCase2_limitOne_isInfeasible() {
        assertThrows(NoFeasibleAssignmentException.class, () -> solver.solve(testCase2(), 1));
    }

    // ------------------------------------------------- first PDF example (10 lanes)

    @ParameterizedTest
    @CsvSource({"1", "2", "3", "4"})
    void specExample_belowFiveTransporters_isInfeasible(int limit) {
        // The PDF's own example needs at least 5 transporters, so limits 1-4 cannot work.
        assertThrows(NoFeasibleAssignmentException.class, () -> solver.solve(specExample(), limit));
    }

    @Test
    void specExample_limitFive_costs48000() {
        AssignmentResult r = solver.solve(specExample(), 5);

        assertEquals(48000, r.totalCost());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), selected(r));
    }

    @Test
    void specExample_limitSeven_doesNotReportTransportersThatCarryNothing() {
        AssignmentResult r = solver.solve(specExample(), 7);

        assertEquals(48000, r.totalCost());
        assertEquals(List.of(1L, 2L, 3L, 4L, 5L), selected(r)); // T6 and T7 are never cheaper
    }

    // -------------------------------------------------- Test Case 1 (PDF, dense)

    @ParameterizedTest
    @CsvSource({"1,96940", "2,62938", "3,60139", "4,59896", "5,59896", "7,59896"})
    void testCase1_optimalCostForEachLimit(int limit, long expectedCost) {
        assertEquals(expectedCost, solver.solve(testCase1(), limit).totalCost());
    }

    @Test
    void testCase1_limitThree_picksOneFourFive() {
        AssignmentResult r = solver.solve(testCase1(), 3);

        assertEquals(60139, r.totalCost());
        assertEquals(List.of(1L, 4L, 5L), selected(r));
    }

    @Test
    void testCase1_limitSeven_reportsOnlyTheFourTransportersActuallyUsed() {
        assertEquals(List.of(1L, 3L, 4L, 5L), selected(solver.solve(testCase1(), 7)));
    }

    // ------------------------------------------------------------------- edge cases

    @Test
    void laneNobodyQuoted_isInfeasible() {
        QuoteMatrix m = matrix(new long[]{10, 20}, new long[]{1}, new long[][]{{500, X}});

        assertThrows(NoFeasibleAssignmentException.class, () -> solver.solve(m, 1));
    }

    @Test
    void equalQuotes_goToTheLowerTransporterId() {
        QuoteMatrix m = matrix(new long[]{1}, new long[]{7, 3}, new long[][]{{100}, {100}});

        AssignmentResult r = solver.solve(m, 2);

        assertEquals(100, r.totalCost());
        assertEquals(3L, laneToTransporter(r).get(1L));
    }

    @Test
    void singleTransporterQuotingEverything_isChosenWhenLimitIsOne() {
        QuoteMatrix m = matrix(new long[]{1, 2}, new long[]{1, 2},
                new long[][]{{300, 300}, {X, 100}});

        AssignmentResult r = solver.solve(m, 1);

        assertEquals(600, r.totalCost());
        assertEquals(List.of(1L), selected(r));
    }

    @Test
    void invalidArguments_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> solver.solve(null, 3));
        assertThrows(IllegalArgumentException.class, () -> solver.solve(testCase2(), 0));
        assertThrows(IllegalArgumentException.class, () -> solver.solve(testCase2(), -4));
    }

    @Test
    void malformedMatrix_isRejected() {
        QuoteMatrix wrongColumns = matrix(new long[]{1, 2}, new long[]{1}, new long[][]{{100}});
        QuoteMatrix wrongRows = matrix(new long[]{1}, new long[]{1, 2}, new long[][]{{100}});
        QuoteMatrix badQuote = matrix(new long[]{1}, new long[]{1}, new long[][]{{-5}});

        assertThrows(IllegalArgumentException.class, () -> solver.solve(wrongColumns, 1));
        assertThrows(IllegalArgumentException.class, () -> solver.solve(wrongRows, 1));
        assertThrows(IllegalArgumentException.class, () -> solver.solve(badQuote, 1));
    }

    @Test
    void emptyInput_isNotFeasible() {
        QuoteMatrix empty = matrix(new long[]{}, new long[]{}, new long[][]{});

        assertThrows(NoFeasibleAssignmentException.class, () -> solver.solve(empty, 3));
    }

    @Test
    void largeDenseInput_finishesQuickly() {
        QuoteMatrix big = randomDense(30, 20, 1234L);

        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertDoesNotThrow(() -> solver.solve(big, 5)));
    }

    // -------------------------------------------------------------------- helpers

    private static Map<Long, Long> laneToTransporter(AssignmentResult r) {
        Map<Long, Long> map = new HashMap<>();
        for (LaneAssignment a : assignments(r)) {
            map.put(a.laneId(), a.transporterId());
        }
        return map;
    }

    private static QuoteMatrix randomDense(int transporters, int lanes, long seed) {
        Random rnd = new Random(seed);
        long[] laneIds = new long[lanes];
        long[] transporterIds = new long[transporters];
        long[][] values = new long[transporters][lanes];
        for (int l = 0; l < lanes; l++) {
            laneIds[l] = l + 1;
        }
        for (int t = 0; t < transporters; t++) {
            transporterIds[t] = t + 1;
            for (int l = 0; l < lanes; l++) {
                values[t][l] = 1000 + rnd.nextInt(9000);
            }
        }
        return matrix(laneIds, transporterIds, values);
    }
}