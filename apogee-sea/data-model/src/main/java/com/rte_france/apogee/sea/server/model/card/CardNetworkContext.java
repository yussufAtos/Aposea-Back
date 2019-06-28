package com.rte_france.apogee.sea.server.model.card;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CardNetworkContext {

    protected Long date;

    protected Map<String, Object> detail;

    public CardNetworkContext(Long date, Map<String, Object> detail) {
        this.date = date;
        this.detail = detail;
    }
}
