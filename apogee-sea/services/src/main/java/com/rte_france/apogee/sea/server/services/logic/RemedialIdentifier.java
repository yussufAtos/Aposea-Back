package com.rte_france.apogee.sea.server.services.logic;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * RemedialIdentifier
 */
@EqualsAndHashCode
@ToString
public class RemedialIdentifier {

    private String idLogicContext;

    private String idAbstractLogic;

    private String shortDescription;

    public RemedialIdentifier idLogicContext(String idLogicContext) {
        this.idLogicContext = idLogicContext;
        return this;
    }
    /**
     * Get idLogicContext
     *
     * @return idLogicContext
     **/

    public String getIdLogicContext() {
        return idLogicContext;
    }

    public void setIdLogicContext(String idLogicContext) {
        this.idLogicContext = idLogicContext;
    }

    public RemedialIdentifier idAbstractLogic(String idAbstractLogic) {
        this.idAbstractLogic = idAbstractLogic;
        return this;
    }
    /**
     * Get idAbstractLogic
     *
     * @return idAbstractLogic
     **/

    public String getIdAbstractLogic() {
        return idAbstractLogic;
    }

    public void setIdAbstractLogic(String idAbstractLogic) {
        this.idAbstractLogic = idAbstractLogic;
    }

    public RemedialIdentifier shortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
        return this;
    }
    /**
     * Get shortDescription
     *
     * @return shortDescription
     **/

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

}
