package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.Data;

@Data
public class ContingencyViolation {

    @JsonView(Views.Public.class)
    NetworkContingency contingency;

    @JsonView(Views.Public.class)
    private int candidateRemedialsCount;

    @JsonView(Views.Public.class)
    private int computedRemedialsCount;

    @JsonView(Views.Public.class)
    private int efficientRemedialsCount;

    @JsonView(Views.Public.class)
    ArraySnapshotContingencyContext violationStatus;
}
