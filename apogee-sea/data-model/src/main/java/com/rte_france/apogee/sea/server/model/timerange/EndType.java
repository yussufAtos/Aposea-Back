package com.rte_france.apogee.sea.server.model.timerange;

/**
 * End time types that can be used
 */
public enum EndType {
    /**
     * MIDNIGHT: the end time is set to next midnight + endTimeDay days
     */
    MIDNIGHT,

    /**
     * HOURRELATIVE, the end time is set to the current time + endTimeHour hours
     */
    HOURRELATIVE,
}
