package com.rte_france.apogee.sea.server.services.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemedialIdentifierTest {

    @Test
    public void idLogicContextTest() {
        RemedialIdentifier remedialIdentifier = new RemedialIdentifier();
        remedialIdentifier.idLogicContext("idLogicContext");
        assertEquals("idLogicContext", remedialIdentifier.getIdLogicContext());
        remedialIdentifier.setIdLogicContext("idLogicContext1");
        assertEquals("idLogicContext1", remedialIdentifier.getIdLogicContext());

        remedialIdentifier.idAbstractLogic("idAbstractLogic");
        assertEquals("idAbstractLogic", remedialIdentifier.getIdAbstractLogic());
        remedialIdentifier.setIdAbstractLogic("idAbstractLogic1");
        assertEquals("idAbstractLogic1", remedialIdentifier.getIdAbstractLogic());

        remedialIdentifier.shortDescription("shortDescription");
        assertEquals("shortDescription", remedialIdentifier.getShortDescription());
        remedialIdentifier.setShortDescription("shortDescription1");
        assertEquals("shortDescription1", remedialIdentifier.getShortDescription());

    }
}
