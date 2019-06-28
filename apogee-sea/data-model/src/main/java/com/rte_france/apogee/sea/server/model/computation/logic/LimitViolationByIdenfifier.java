package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.Data;

import java.util.List;

@Data
public class LimitViolationByIdenfifier {

    @JsonView(Views.Public.class)
    NetworkLimitViolationIdentifier identifier;

    @JsonView(Views.Public.class)
    List<NetworkContextLimitViolation> limitViolations;
}
