package com.rte_france.apogee.sea.server.exceptions;

public class DataDeleteViolationException extends RuntimeException {

    public DataDeleteViolationException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public DataDeleteViolationException(String message) {
        super(message, null);
    }

}
