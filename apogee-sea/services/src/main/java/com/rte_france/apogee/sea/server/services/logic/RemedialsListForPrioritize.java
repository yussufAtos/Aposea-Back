package com.rte_france.apogee.sea.server.services.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import lombok.Data;

import java.util.List;

@Data
public class RemedialsListForPrioritize {
    @JsonView(Views.Public.class)
    List<Remedial> allRemedials;
    @JsonView(Views.Public.class)
    List<Remedial> candidatesRemedials;
    @JsonView(Views.Public.class)
    List<Prioritize> prioritizedRemedials;
}
