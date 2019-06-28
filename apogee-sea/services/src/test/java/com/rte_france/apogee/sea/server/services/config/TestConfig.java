package com.rte_france.apogee.sea.server.services.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component(value = "testProperties")
@PropertySource("classpath:test.config.properties")
@ConfigurationProperties("test")
public class TestConfig extends AbstractRestServiceConfig {

    public static final String SERVICE_1 = "Service1";
    public static final String SERVICE_2 = "Service2";
    public static final String SERVICE_3 = "Service3";

}
