package com.rte_france.apogee.sea.server.model.computation;

import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RemedialTest {

    @Test
    public void test() {
        Remedial remedial = new Remedial("idRemedialRepository", "shortDescription", new ArrayList<>());
        assertEquals("idRemedialRepository", remedial.getIdRemedialRepository());
        assertEquals("shortDescription", remedial.getShortDescription());

        Remedial remedial1 = new Remedial("idRemedialRepository1", "shortDescription1");
        assertEquals("idRemedialRepository1", remedial1.getIdRemedialRepository());
        assertEquals("shortDescription1", remedial1.getShortDescription());
    }
}
