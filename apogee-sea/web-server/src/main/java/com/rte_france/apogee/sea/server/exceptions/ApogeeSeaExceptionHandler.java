package com.rte_france.apogee.sea.server.exceptions;

import com.rte_france.apogee.sea.server.services.exceptions.SnapshotNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ApogeeSeaExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApogeeSeaExceptionHandler.class);

    @ExceptionHandler(DataRetrievalException.class)
    public ResponseEntity<Error> dataRetrieval(DataRetrievalException e) {
        trace(e);
        return new ResponseEntity<>(new Error(1, "Error retrieving the sent data", e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Error> resourceNotFound(ResourceNotFoundException e) {
        trace(e);
        return new ResponseEntity<>(new Error(1, "The resource can't be found", e.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SnapshotNotFoundException.class)
    public ResponseEntity<Error> snapshotNotFound(SnapshotNotFoundException e) {
        trace(e);
        return new ResponseEntity<>(new Error(1, "The snapshot can't be found", e.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataDeleteViolationException.class)
    public ResponseEntity<Error> dataIntegrityViolation(Exception e) {
        trace(e);
        return new ResponseEntity<>(new Error(1, "The resource can't be deleted", e.getMessage()),
                HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<Error> alreadyExistsViolation(Exception e) {
        trace(e);
        return new ResponseEntity<>(new Error(1, "The resource cannot be created because it already exists", e.getMessage()),
                HttpStatus.BAD_REQUEST);
    }

    private void trace(Throwable e) {
        LOGGER.error("An error occurred", e);
    }

}
