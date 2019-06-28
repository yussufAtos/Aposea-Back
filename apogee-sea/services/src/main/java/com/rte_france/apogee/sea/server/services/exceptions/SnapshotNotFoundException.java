package com.rte_france.apogee.sea.server.services.exceptions;

/**
 * SnapshotNotFoundException custom exception for snapshot not found
 */
public class SnapshotNotFoundException extends RuntimeException {
    /**
     * @param message detail message of the exception
     */
    public SnapshotNotFoundException(String message) {
        super(message);
    }
}
