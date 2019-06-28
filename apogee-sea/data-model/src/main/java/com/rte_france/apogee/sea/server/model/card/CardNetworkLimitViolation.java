package com.rte_france.apogee.sea.server.model.card;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class CardNetworkLimitViolation {

    protected String name;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected List<CardNetworkContext> networkContexts;

    protected List<CardNetworkLimitViolationValueItem> values;

    protected Map<String, String> detail;

    public CardNetworkLimitViolation(String name, List<CardNetworkContext> networkContexts, List<CardNetworkLimitViolationValueItem> values, Map<String, String> detail) {
        this.name = name;
        this.networkContexts = networkContexts;
        this.values = values;
        this.detail = detail;
    }
}
