package com.rte_france.apogee.sea.server.model.timerange;

import lombok.Data;

import java.time.Instant;

@Data
public class TimerangeFilterDate {

    private Instant startDate;
    private Instant endDate;

    public TimerangeFilterDate() {
    }

    public TimerangeFilterDate(Instant startDate, Instant endDate) {
        this.startDate = startDate;
        this.endDate = endDate;

    }

}
