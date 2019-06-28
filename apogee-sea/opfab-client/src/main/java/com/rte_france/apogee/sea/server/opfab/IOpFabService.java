package com.rte_france.apogee.sea.server.opfab;

public interface IOpFabService {

    void pushCards() throws IOpFabService.OpFabServiceException;

    class OpFabServiceException extends java.lang.Exception {
        public OpFabServiceException(String message, Throwable e) {
            super(message, e);
        }

        public OpFabServiceException(String message) {
            super(message);
        }
    }
}
