package com.rte_france.apogee.sea.server.model.timerange;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

/**
 * A generic time range, with configurable start time and end time
 */
@Entity
@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class TimerangeType implements Serializable {

    /**
     * The name of the time range displayed to the user
     */
    @Id
    @NonNull
    @Column(length = 50)
    @JsonView({Views.Public.class})
    private String name;

    /**
     * The time zone to be used when using a startType or endType MIDNIGHT
     */
    @NonNull
    @Column(length = 50)
    private String timeZone;

    /**
     * The start time type
     */
    @Column(length = 16)
    @Enumerated(value = EnumType.STRING)
    protected StartType startType;

    /**
     * The end time type
     */
    @Column(length = 16)
    @Enumerated(value = EnumType.STRING)
    protected EndType endType;

    /**
     * To be set to null if startType=MIDNIGHT.
     * if startType=NOW, set to 30' to have startTime starting at NOW-30', ...
     */
    @Column(length = 3)
    protected Integer startTimeMinutes;

    /**
     * To be set to null if startType=NOW.
     * if startType=MIDNIGHT, set to 1 to have startTime starting at J+1, 2 for J+2, ...
     */
    private Integer startTimeDay;

    /**
     * To be set to null if startType=NOW.
     * if startType=MIDNIGHT, set to 8 to have startTime starting at J+startTimeDay + 8hours
     */
    private Integer startTimeHour;

    /**
     * To be set to null if endType=HOURRELATIVE.
     * if endType=MIDNIGHT, set to 1 to have endTime finishing at next midnight (J+1 00h00)
     */
    private Integer endTimeDay;

    /**
     * To set to 1 to have endType finishing at next midnight (J+1 00h00)
     */
    private Integer endTimeHour;

    /**
     * Optional feature: If the difference between end time and start time is lower than this number of hours,
     * then switch to the another time range specified in alternateTimerange.
     * Must be set to null to disable the feature.
     */
    private Integer alternateIfLessHoursThan;

    /**
     * Optional feature: If the difference between end time and start time is lower than alternateTimerange hours,
     * then switch to this another time range .
     * Must be set to null to disable the feature.
     */
    @OneToOne
    @JoinColumn(name = "alternate_timerange")
    private TimerangeType alternateTimerange;

    /**
     * Enable/Disable the generation of OpFab cards using this type of time range
     */
    private boolean opfabEnabled;

    /**
     * The tag name for OpFab cards
     */
    @NonNull
    private String cardTag;

    /**
     * Minutes to be added to OpFab card start date
     */
    private Integer cardStartDateIncrement;

    /**
     * Minutes to be added to OpFab card end date
     */
    private Integer cardEndDateIncrement;

}
