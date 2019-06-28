package com.rte_france.apogee.sea.server.model;

import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedial;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedialId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrioritizeRemedialTest {

    @Test
    public void testPrioritizeRemedialId() {
        Prioritize prioritize = new Prioritize(Instant.now());
        PrioritizeRemedial prioritizeRemedial = new PrioritizeRemedial(1, prioritize);
        PrioritizeRemedialId prioritizeRemedialId = new PrioritizeRemedialId();
        prioritizeRemedialId.setPrioritize(prioritize);
        prioritizeRemedialId.setPrioritizeValue(1);
        assertEquals(1, prioritizeRemedialId.getPrioritizeValue());
    }
}
