package com.rte_france.apogee.sea.server.opfab;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.jdbc.AutoConfigureDataJdbc;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {OpFabServiceTest.class})
@ContextConfiguration(classes = {OpFabServiceTestConfiguration.class})
@EnableAutoConfiguration
@Transactional
@AutoConfigureDataJpa
@AutoConfigureDataJdbc
@EntityScan("com.rte_france.apogee.sea.server")
@ComponentScan({"com.rte_france.apogee.sea.server", "com.rte_france.apogee.sea.server.opfab.util"})
@EnableJpaRepositories("com.rte_france.apogee.sea.server")
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
@EnableFeignClients
public class OpFabServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpFabServiceTest.class);

    private WireMockServer wireMockServer;

    @Autowired
    UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @Autowired
    OpFabService opFabService;

    @BeforeEach
    public void setUp() throws Exception {
        //wireMockServer
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();

        //handle Instant.now()
        String instantExpected = "2018-10-16T07:00:00Z";
        Clock clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.systemDefault());
        Instant instant = Instant.now(clock);

        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };
    }

    private void setupStub(int delay) {
        wireMockServer.stubFor(post(urlEqualTo("/async/cards"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withFixedDelay(delay)));
    }

    @Test
    @Sql({"/test-context-data.sql"})
    public void pushCards() throws IOpFabService.OpFabServiceException {
        setupStub(0);

        uiSnapshotDaoImpl.handleUiSnapshotCreation();

        opFabService.pushCards();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/async/cards"))
                .withRequestBody(matchingJsonPath("$[?(@.processId == 'CASETYPE-PF-TOULOUSE-ALARM' || @.processId == 'TIMERANGE-TR-TOULOUSE-ALARM')]")));

    }

    @Test
    @Sql({"/test-context-data.sql"})
    public void pushCardsWithDelay() throws IOpFabService.OpFabServiceException {
        setupStub(10000);

        uiSnapshotDaoImpl.handleUiSnapshotCreation();

        boolean exceptionThrown = false;

        try {
            opFabService.pushCards();
        } catch (feign.RetryableException e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown, "feign.RetryableException should be throw");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/async/cards")));

    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }
}
