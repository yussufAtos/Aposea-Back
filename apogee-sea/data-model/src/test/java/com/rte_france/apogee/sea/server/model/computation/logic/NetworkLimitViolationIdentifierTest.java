package com.rte_france.apogee.sea.server.model.computation.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetworkLimitViolationIdentifierTest {

    @Test
    public void idLogicContextTest() {
        NetworkLimitViolationIdentifier networkLimitViolationIdentifier = new NetworkLimitViolationIdentifier();
        networkLimitViolationIdentifier.setSubjectId("subjectId");
        networkLimitViolationIdentifier.setLimitType("limitType");
        networkLimitViolationIdentifier.setAcceptableDuration(1000);
        networkLimitViolationIdentifier.setSide("side");
        assertEquals("subjectId", networkLimitViolationIdentifier.getSubjectId());

        NetworkLimitViolationIdentifier networkLimitViolationIdentifier1 = new NetworkLimitViolationIdentifier("subjectId",
                "limitType", 1000, "side1");
        assertEquals("subjectId", networkLimitViolationIdentifier1.getSubjectId());
        assertEquals(new Integer(1000), networkLimitViolationIdentifier1.getAcceptableDuration());

        assertEquals(-1, networkLimitViolationIdentifier.compareTo(networkLimitViolationIdentifier1));


    }
}
