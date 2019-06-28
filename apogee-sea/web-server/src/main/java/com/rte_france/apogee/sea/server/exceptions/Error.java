package com.rte_france.apogee.sea.server.exceptions;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Error {
    private int code;
    private String message;
    private String cause;
}

