package com.rte_france.apogee.sea.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

/**
 * An Authorization Server will more typically have a key rotation strategy, and the keys will not
 * be hard-coded into the application code.
 * <p>
 * For simplicity, though, this sample doesn't demonstrate key rotation.
 */
@Configuration
class KeyConfig {

    private KeyProperties keyProperties;

    @Autowired
    public KeyConfig(KeyProperties keyProperties) {
        this.keyProperties = keyProperties;
    }


    @Bean
    KeyPair keyPair() {
        try {
            RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(new BigInteger(keyProperties.getModulus()), new BigInteger(keyProperties.getExponent()));
            RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(new BigInteger(keyProperties.getModulus()), new BigInteger(keyProperties.getPrivateExponent()));
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return new KeyPair(factory.generatePublic(publicSpec), factory.generatePrivate(privateSpec));
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
