package com.rte_france.apogee.sea.server.model.remedials;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class PrioritizeRemedialId implements Serializable {
    @Getter
    @Setter
    private int prioritizeValue;
    @Getter
    @Setter
    private Prioritize prioritize;
}
