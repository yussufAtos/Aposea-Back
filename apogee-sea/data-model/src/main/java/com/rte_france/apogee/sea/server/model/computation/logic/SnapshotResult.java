package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class SnapshotResult {

    @JsonView(Views.Public.class)
    private UiSnapshot uiSnapshot;

    @JsonView(Views.Public.class)
    private List<ContingencyViolation> contingencies;

    @JsonView(Views.UiSnapshot.class)
    private List<NetworkContext> networkContexts;

    @JsonView(Views.Public.class)
    private List<LimitViolationByIdenfifier> preContingencyLimitViolations;

    @JsonView(Views.Public.class)
    private int pageNumber;

    @JsonView(Views.Public.class)
    private int totalPages;

    @JsonView(Views.Public.class)
    private int totalRows;
}
