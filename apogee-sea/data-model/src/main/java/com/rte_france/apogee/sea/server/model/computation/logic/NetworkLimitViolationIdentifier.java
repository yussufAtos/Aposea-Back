package com.rte_france.apogee.sea.server.model.computation.logic;

import com.fasterxml.jackson.annotation.JsonView;
import com.rte_france.apogee.sea.server.model.jview.Views;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class NetworkLimitViolationIdentifier implements Comparable {

    @JsonView(Views.Public.class)
    private String subjectId;

    @JsonView(Views.Public.class)
    private String limitType;

    @JsonView(Views.Public.class)
    private Integer acceptableDuration;

    @JsonView(Views.Public.class)
    private String side;

    public NetworkLimitViolationIdentifier() {
    }

    public NetworkLimitViolationIdentifier(String subjectId, String limitType, Integer acceptableDuration, String side) {
        this.subjectId = subjectId;
        this.limitType = limitType;
        this.acceptableDuration = acceptableDuration;
        this.side = side;
    }


    public int compareTo(Object o) {
        NetworkLimitViolationIdentifier other = (NetworkLimitViolationIdentifier) o;
        int result = this.subjectId.compareTo(other.getSubjectId());
        if (result != 0) {
            return result;
        } else {
            result = this.limitType.compareTo(other.getLimitType());
        }
        if (result != 0) {
            return result;
        } else {
            result = this.acceptableDuration.compareTo(other.getAcceptableDuration());
        }
        if (result != 0) {
            return result;
        } else {
            result = this.side.compareTo(other.getSide());
        }
        return result;
    }
}
