package com.rte_france.apogee.sea.server.model.card;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CardNetworkRemedial {

    protected String name;

    //Violation post remedial
    protected List<CardNetworkLimitViolation> networkLimitViolations;

    protected Map<String, String> detail;

    public CardNetworkRemedial(String name, List<CardNetworkLimitViolation> networkLimitViolations, Map<String, String> detail) {
        this.name = name;
        this.networkLimitViolations = networkLimitViolations;
        this.detail = detail;
    }
}
