package com.rte_france.apogee.sea.server.services.logic;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedial;
import lombok.Data;

import java.util.List;

@Data
public class PrioritizeRemedialByContingency {
    NetworkContingency contingency;
    String startDate;
    String endDate;
    List<PrioritizeRemedial> prioritizedRemedials;
}
