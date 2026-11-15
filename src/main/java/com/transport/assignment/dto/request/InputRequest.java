package com.transport.assignment.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;


import java.util.List;

public record InputRequest(

        @NotEmpty 
        List<@Valid LaneRequest> lanes,

        @NotEmpty
        List<@Valid TransporterRequest> transporters

) {
}
