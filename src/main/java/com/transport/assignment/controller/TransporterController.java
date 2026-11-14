package com.transport.assignment.controller;

import com.transport.assignment.dto.request.InputRequest;
import com.transport.assignment.dto.response.InputSavedResponse;
import com.transport.assignment.service.InputService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transporters")
@RequiredArgsConstructor
public class TransporterController {

    private final InputService inputService;

    @PostMapping("/input")
    public ResponseEntity<InputSavedResponse> saveInput(
            @Valid @RequestBody InputRequest request
    ) {

        InputSavedResponse response =
                inputService.saveInput(request);

        return ResponseEntity.ok(response);
    }
}
