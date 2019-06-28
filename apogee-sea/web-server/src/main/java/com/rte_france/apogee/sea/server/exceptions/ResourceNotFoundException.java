package com.rte_france.apogee.sea.server.exceptions;

public class ResourceNotFoundException  extends RuntimeException {

    public ResourceNotFoundException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public ResourceNotFoundException(String message) {
        super(message, null);
    }

}
