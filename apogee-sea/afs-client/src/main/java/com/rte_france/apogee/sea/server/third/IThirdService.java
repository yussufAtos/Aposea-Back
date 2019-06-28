package com.rte_france.apogee.sea.server.third;

import org.springframework.web.multipart.MultipartFile;

public interface IThirdService {

    void saveComputationResult(MultipartFile saresult, MultipartFile remedialfile) throws IThirdService.ThirdServiceException;

    void retrieveAndSaveRemedials(String idAfsRunner) throws ThirdServiceException;

    class ThirdServiceException extends java.lang.Exception {
        public ThirdServiceException(String message, Throwable e) {
            super(message, e);
        }
    }
}
