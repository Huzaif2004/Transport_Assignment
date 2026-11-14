package com.transport.assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InputRequest(

        @NotNull
        List<@Valid LaneRequest> lanes,

        @NotNull
        List<@Valid TransporterRequest> transporters

) {
}
