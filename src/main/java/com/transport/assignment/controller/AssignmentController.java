package com.transport.assignment.controller;

import com.transport.assignment.dto.request.AssignmentRequest;
import com.transport.assignment.dto.response.AssignmentResponse;
import com.transport.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping("/calculate")
    public ResponseEntity<AssignmentResponse> calculate(
            @Valid @RequestBody AssignmentRequest request
    ) {

        AssignmentResponse response =
                assignmentService.calculate(request);

        return ResponseEntity.ok(response);
    }
}
