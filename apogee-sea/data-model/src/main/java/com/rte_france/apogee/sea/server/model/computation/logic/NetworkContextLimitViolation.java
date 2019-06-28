package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolation;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.Data;

@Data
public class NetworkContextLimitViolation {

    @JsonView(Views.Public.class)
    Long contextId;

    @JsonView(Views.Public.class)
    NetworkLimitViolation limitViolation;

}
