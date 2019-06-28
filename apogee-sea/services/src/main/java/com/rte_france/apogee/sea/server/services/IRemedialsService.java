package com.rte_france.apogee.sea.server.services;

import com.powsybl.security.SecurityAnalysisResult;
import com.rte_france.itesla.variant.result.VariantSimulatorResult;

import java.time.Instant;

public interface IRemedialsService {

    void retrieveAndSaveRemedials(VariantSimulatorResult variantSimulatorResult, Long computationResultId) throws RemedialServiceException;

    void retrieveAndSaveRemedials(SecurityAnalysisResult securityAnalysisResult, Long computationResultId) throws RemedialServiceException;

    String retrieveIalCodeRemedial(Instant networkDate) throws RemedialServiceException;

    void saveRemedials(String jsonString, Long computationResultId) throws RemedialServiceException;

    class RemedialServiceException extends java.lang.Exception {
        public RemedialServiceException(String message, Throwable e) {
            super(message, e);
        }

    }


}
