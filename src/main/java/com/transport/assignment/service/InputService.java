package com.transport.assignment.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.assignment.dto.request.InputRequest;
import com.transport.assignment.dto.request.LaneQuoteRequest;
import com.transport.assignment.dto.request.LaneRequest;
import com.transport.assignment.dto.request.TransporterRequest;
import com.transport.assignment.dto.response.InputSavedResponse;
import com.transport.assignment.entity.Lane;
import com.transport.assignment.entity.LaneQuote;
import com.transport.assignment.entity.Transporter;
import com.transport.assignment.exception.InvalidInputException;
import com.transport.assignment.repository.LaneQuoteRepository;
import com.transport.assignment.repository.LaneRepository;
import com.transport.assignment.repository.TransporterRepository;


@Service
public class InputService {

        private final LaneRepository laneRepository;
        private final TransporterRepository transporterRepository;
        private final LaneQuoteRepository laneQuoteRepository;
        public InputService(LaneRepository laneRepository, TransporterRepository transporterRepository, LaneQuoteRepository laneQuoteRepository) {
                this.laneRepository = laneRepository;
                this.transporterRepository = transporterRepository;
                this.laneQuoteRepository = laneQuoteRepository;
        }

        @Transactional
        public InputSavedResponse saveInput(InputRequest request) {

                // Step 1: Validate the input before modifying the database
                validateInput(request);

                // Step 2: Remove the previous dataset.
                // Child records are deleted first before parent records.
                laneQuoteRepository.deleteAllInBatch();
                transporterRepository.deleteAllInBatch();
                laneRepository.deleteAllInBatch();

                // Convert LaneRequest DTOs into Lane entities
                List<Lane> lanes = request.lanes()
                                .stream()
                                .map(laneRequest -> new Lane(
                                                laneRequest.id(),
                                                laneRequest.origin(),
                                                laneRequest.destination()))
                                .toList();

                // Convert TransporterRequest DTOs into entities
                List<Transporter> transporters = request.transporters()
                                .stream()
                                .map(transporterRequest -> new Transporter(
                                                transporterRequest.id(),
                                                transporterRequest.name()))
                                .toList();

                // Persist lanes and transporters
                List<Lane> savedLanes = laneRepository.saveAllAndFlush(lanes);

                List<Transporter> savedTransporters = transporterRepository.saveAllAndFlush(transporters);

                // Create lookup maps using database IDs
                Map<Long, Lane> laneMap = savedLanes.stream()
                                .collect(Collectors.toMap(
                                                Lane::getId,
                                                Function.identity()));

                Map<Long, Transporter> transporterMap = savedTransporters.stream()
                                .collect(Collectors.toMap(
                                                Transporter::getId,
                                                Function.identity()));

                // Convert nested lane quotes into LaneQuote entities

                List<LaneQuote> quotes = new ArrayList<>();

                for (TransporterRequest transporterRequest : request.transporters()) {

                        Transporter transporter = transporterMap.get(transporterRequest.id());

                        for (LaneQuoteRequest quoteRequest : transporterRequest.laneQuotes()) {

                                Lane lane = laneMap.get(quoteRequest.laneId());

                                LaneQuote quote = new LaneQuote(
                                                transporter,
                                                lane,
                                                quoteRequest.quote());

                                quotes.add(quote);
                        }
                }

                

                // Persist all lane quotes
                laneQuoteRepository.saveAll(quotes);

                return new InputSavedResponse(
                                "success",
                                "Input data saved successfully.");
        }

        private void validateInput(InputRequest request) {

                if (request == null ||
                                request.lanes() == null ||
                                request.lanes().isEmpty() ||
                                request.transporters() == null ||
                                request.transporters().isEmpty()) {

                        throw new InvalidInputException(
                                        "Lanes and transporters must not be empty.");
                }

                // Validate duplicate lane IDs
                Set<Long> laneIds = new HashSet<>();

                for (LaneRequest lane : request.lanes()) {

                        if (lane.id() == null || !laneIds.add(lane.id())) {
                                throw new InvalidInputException(
                                                "Invalid or duplicate lane ID: " + lane.id());
                        }
                }

                // Validate duplicate transporter IDs
                Set<Long> transporterIds = new HashSet<>();

                for (TransporterRequest transporter : request.transporters()) {

                        if (transporter.id() == null ||
                                        !transporterIds.add(transporter.id())) {

                                throw new InvalidInputException(
                                                "Invalid or duplicate transporter ID: "
                                                                + transporter.id());
                        }

                        if (transporter.laneQuotes() == null) {
                                throw new InvalidInputException(
                                                "Lane quotes cannot be null for transporter: "
                                                                + transporter.id());
                        }

                        // A transporter must not quote the same lane twice
                        Set<Long> quotedLaneIds = new HashSet<>();

                        for (LaneQuoteRequest quote : transporter.laneQuotes()) {

                                if (quote.laneId() == null ||
                                                !laneIds.contains(quote.laneId())) {

                                        // Note: laneIds is fully populated before this loop.
                                        throw new InvalidInputException(
                                                        "Unknown lane ID: " + quote.laneId());
                                }

                                if (!quotedLaneIds.add(quote.laneId())) {
                                        throw new InvalidInputException(
                                                        "Duplicate quote for lane "
                                                                        + quote.laneId()
                                                                        + " by transporter "
                                                                        + transporter.id());
                                }

                                if (quote.quote() == null || quote.quote() <= 0) {
                                        throw new InvalidInputException(
                                                        "Quote must be greater than zero.");
                                }
                        }
                }
        }
}
