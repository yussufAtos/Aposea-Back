package com.rte_france.apogee.sea.server.model.card;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CardNetworkContingency {

    protected String name;

    //Violations N-K
    protected List<CardNetworkLimitViolation> networkLimitViolations;

    protected List<CardNetworkRemedial> networkRemedials;

    protected Map<String, String> detail;

    public CardNetworkContingency(String name, List<CardNetworkLimitViolation> networkLimitViolations, List<CardNetworkRemedial> networkRemedials, Map<String, String> detail) {
        this.name = name;
        this.networkLimitViolations = networkLimitViolations;
        this.networkRemedials = networkRemedials;
        this.detail = detail;
    }
}
