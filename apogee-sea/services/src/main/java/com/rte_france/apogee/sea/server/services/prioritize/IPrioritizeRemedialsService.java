package com.rte_france.apogee.sea.server.services.prioritize;

import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.logic.RemedialIdentifier;
import com.rte_france.apogee.sea.server.services.logic.RemedialsListForPrioritize;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface IPrioritizeRemedialsService {

    List<Remedial> getRemedials(String contingencyId);

    void deleteRemedials(String remedialsId) throws PrioritizeRemedialServiceException;

    List<Prioritize> getPrioritizeRemedial(String endDate, String contingencyId) throws IPrioritizeRemedialsService.PrioritizeRemedialServiceException;

    RemedialsListForPrioritize getRemedialsListForPrioritize(String prioritizeDate, String contingencyId, String contextId) throws PrioritizeRemedialServiceException;

    void deleteByNetworkContingency(String contingencyId);

    Map<String, List<RemedialIdentifier>> findRemedialsPrioritizeByNetworkDate(Instant networkDate);

    void savePrioritizeRemedial(List<Prioritize> prioritizeRemedial) throws PrioritizeRemedialServiceException;

    class PrioritizeRemedialServiceException extends java.lang.Exception {
        public PrioritizeRemedialServiceException(String message, Throwable e) {
            super(message, e);
        }

        public PrioritizeRemedialServiceException(String message) {
            super(message);
        }

    }
}
