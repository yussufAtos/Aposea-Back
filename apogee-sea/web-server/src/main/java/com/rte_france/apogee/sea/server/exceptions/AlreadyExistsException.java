package com.rte_france.apogee.sea.server.exceptions;

public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

}
