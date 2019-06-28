package com.rte_france.apogee.sea.server.services.config;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class Authentication {

    private Authentication() {
    }

    public static String basicAuthHeader(String username, String password) {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);

        String usernameAndPassword = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(usernameAndPassword.getBytes(StandardCharsets.US_ASCII));
        return "Basic " + new String(encodedAuth);
    }

}
