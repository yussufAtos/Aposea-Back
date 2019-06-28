package com.rte_france.apogee.sea.server.services.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@EnableAutoConfiguration
@ComponentScan
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RestServiceConfigTest.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EntityScan("com.rte_france.apogee.sea.server")
@TestPropertySource(locations = "classpath:test.config.properties")
public class RestServiceConfigTest {

    @Autowired
    @Qualifier("testProperties")
    AbstractRestServiceConfig config;

    @Test
    public void testUri() {
        URI uri = config.asUri();

        assertEquals("http", uri.getScheme());
        assertEquals("host", uri.getHost());
        assertEquals(1234, uri.getPort());

        config.setSecure(Boolean.TRUE);
        assertEquals("https", config.asUri().getScheme());
    }


    @Test
    public void getServicePath() {
        Optional<String> service = config.getServicePath(TestConfig.SERVICE_1);
        assertThat(service).isPresent();
        service.ifPresent(z -> assertThat(z).isEqualTo("serviceAddress1"));

        service = config.getServicePath(TestConfig.SERVICE_2);
        assertThat(service).isPresent();
        service.ifPresent(z -> assertThat(z).isEqualTo("serviceAddress2"));

        service = config.getServicePath(TestConfig.SERVICE_3);
        assertThat(service).isNotPresent();
    }


    @Test
    public void getBasicAuthentication() {
        assertEquals("Basic c29tZVVzZXJuYW1lOnNvbWVQYXNzd29yZA==", config.getBasicAuthentication());

        config.setPassword(null);
        assertEquals("Basic c29tZVVzZXJuYW1lOg==", config.getBasicAuthentication());
    }

}
