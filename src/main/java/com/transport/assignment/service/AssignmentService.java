package com.transport.assignment.service;

import com.transport.assignment.dto.request.AssignmentRequest;
import com.transport.assignment.dto.response.AssignmentResponse;
import com.transport.assignment.dto.response.LaneAssignmentResponse;
import com.transport.assignment.entity.Lane;
import com.transport.assignment.entity.LaneQuote;
import com.transport.assignment.entity.Transporter;
import com.transport.assignment.exception.InputDataNotFoundException;
import com.transport.assignment.exception.InvalidInputException;
import com.transport.assignment.repository.LaneQuoteRepository;
import com.transport.assignment.repository.LaneRepository;
import com.transport.assignment.repository.TransporterRepository;
import com.transport.assignment.solver.AssignmentResult;
import com.transport.assignment.solver.AssignmentSolver;
import com.transport.assignment.solver.QuoteMatrix;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;

    private final AssignmentSolver solver;

    @Transactional(readOnly = true)
    public AssignmentResponse calculate(AssignmentRequest request) {

        // Step 1: Validate request
        if (request == null || request.maxTransporters() < 1) {
            throw new InvalidInputException(
                    "Maximum transporters must be at least 1."
            );
        }

        // Step 2: Fetch saved data in deterministic ID order
        List<Lane> lanes = laneRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );

        List<Transporter> transporters = transporterRepository.findAll(
                Sort.by(Sort.Direction.ASC, "id")
        );

        if (lanes.isEmpty() || transporters.isEmpty()) {
            throw new InputDataNotFoundException(
                    "No input data found. Submit transporter data first."
            );
        }

        // Step 3: Fetch all quotes along with their related entities
        List<LaneQuote> quotes =
                laneQuoteRepository.findAllWithDetails();

        // Step 4: Convert database data into a QuoteMatrix
        QuoteMatrix matrix = buildQuoteMatrix(
                lanes,
                transporters,
                quotes
        );

        // Step 5: Invoke the solver
        AssignmentResult result = solver.solve(
                matrix,
                request.maxTransporters()
        );

        // Step 6: Convert solver result into API response DTOs
        List<LaneAssignmentResponse> assignments =
                result.assignments()
                        .stream()
                        .map(assignment ->
                                new LaneAssignmentResponse(
                                        assignment.laneId(),
                                        assignment.transporterId()
                                )
                        )
                        .toList();

        return new AssignmentResponse(
                "success",
                result.totalCost(),
                assignments,
                result.selectedTransporters()
        );
    }


    private QuoteMatrix buildQuoteMatrix(
            List<Lane> lanes,
            List<Transporter> transporters,
            List<LaneQuote> quotes) {

        // Preserve the deterministic ordering from the repository results
        List<Long> laneIds = lanes.stream()
                .map(Lane::getId)
                .toList();

        List<Long> transporterIds = transporters.stream()
                .map(Transporter::getId)
                .toList();

        // Build ID -> matrix index maps
        Map<Long, Integer> laneIndex = new HashMap<>();

        for (int i = 0; i < laneIds.size(); i++) {
            laneIndex.put(laneIds.get(i), i);
        }

        Map<Long, Integer> transporterIndex = new HashMap<>();

        for (int i = 0; i < transporterIds.size(); i++) {
            transporterIndex.put(transporterIds.get(i), i);
        }

        // Initialize all missing quotes to -1
        long[][] values = new long[
                transporterIds.size()
        ][
                laneIds.size()
        ];

        for (long[] row : values) {
            Arrays.fill(row, -1L);
        }

        // Populate the quote matrix
        for (LaneQuote quote : quotes) {

            Long laneId = quote.getLane().getId();
            Long transporterId = quote.getTransporter().getId();

            Integer lanePosition = laneIndex.get(laneId);
            Integer transporterPosition =
                    transporterIndex.get(transporterId);

            if (lanePosition == null || transporterPosition == null) {
                throw new IllegalStateException(
                        "Database contains a quote with an unknown "
                                + "lane or transporter."
                );
            }

            values[transporterPosition][lanePosition] =
                    quote.getQuote();
        }

        return new QuoteMatrix(
                laneIds,
                transporterIds,
                values
        );
    }
}
