package com.rte_france.apogee.sea.server.exceptions;

public class DataRetrievalException extends RuntimeException {

    public DataRetrievalException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
