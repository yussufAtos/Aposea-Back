package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LimitViolationByIdenfifierAndRemedials {

    @JsonView(Views.Public.class)
    List<LimitViolationByIdenfifier> violations;

    @JsonView(Views.Public.class)
    List<List<Remedial>> candidatesRemedials;

    @JsonView(Views.Public.class)
    Map<String, RemedialResult> remedialsResults;
}
