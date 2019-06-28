package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.uisnapshot.Status;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RemedialResult {

    @JsonView(Views.Public.class)
    String remedial;

    @JsonView(Views.Public.class)
    List<LimitViolationByIdenfifier> violations = new ArrayList<>();

    @JsonView(Views.Public.class)
    List<StatusByContext> status = new ArrayList<>();

    public RemedialResult(String remedial) {
        this.remedial = remedial;
    }

    @Data
    public static class StatusByContext {
        @JsonView(Views.Public.class)
        Long contextId;

        @JsonView(Views.Public.class)
        Status status;

        public StatusByContext(Long contextId) {
            this.contextId = contextId;
        }
    }
}
