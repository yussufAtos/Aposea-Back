package com.rte_france.apogee.sea.server.model.card;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CardNetworkLimitViolationValueItem {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    protected String value;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Map<String, String> detail;

    public CardNetworkLimitViolationValueItem(String value, Map<String, String> detail) {
        this.value = value;
        this.detail = detail;
    }
}
