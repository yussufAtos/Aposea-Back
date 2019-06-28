package com.rte_france.apogee.sea.server.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class CardData {

    //Violations N
    protected List<CardNetworkLimitViolation> networkLimitViolationsN;

    protected List<CardNetworkContingency> networkContingencies;

    @JsonIgnore
    protected Integer pfPosition;

    protected Map<String, String> detail;


    public CardData(List<CardNetworkLimitViolation> networkLimitViolationsN, List<CardNetworkContingency> networkContingencies, Map<String, String> detail) {
        this.networkLimitViolationsN = networkLimitViolationsN;
        this.networkContingencies = networkContingencies;
        this.detail = detail;
    }
}
