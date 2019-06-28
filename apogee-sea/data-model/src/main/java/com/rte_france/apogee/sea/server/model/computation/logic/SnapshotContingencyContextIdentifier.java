package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.uisnapshot.Status;
import lombok.Data;

@Data
public class SnapshotContingencyContextIdentifier {

    @JsonView(Views.Public.class)
    private Long networkContextId;

    @JsonView(Views.Public.class)
    private Status status;

}
