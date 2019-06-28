package com.rte_france.apogee.sea.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@PropertySource("classpath:keys.properties")
@ConfigurationProperties("config.keys")
public class KeyProperties {
    private String privateExponent;
    private String modulus;
    private String exponent;
}
