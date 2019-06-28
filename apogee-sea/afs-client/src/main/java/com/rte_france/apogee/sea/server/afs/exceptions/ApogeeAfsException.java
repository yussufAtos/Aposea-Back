package com.rte_france.apogee.sea.server.afs.exceptions;

import com.powsybl.commons.PowsyblException;

public class ApogeeAfsException extends PowsyblException {
    public ApogeeAfsException(String message) {
        super(message);
    }

    public ApogeeAfsException(String message, Throwable e) {
        super(message, e);
    }
}
