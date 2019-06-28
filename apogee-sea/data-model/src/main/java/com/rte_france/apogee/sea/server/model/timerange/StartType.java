package com.rte_france.apogee.sea.server.model.timerange;

/**
 * Start time types that can be used
 */
public enum StartType {
    /**
     * NOW: the start time is set to the current time
     */
    NOW,

    /**
     * MIDNIGHT: the end time is set to next midnight + startTimeDay days
     */
    MIDNIGHT
}
