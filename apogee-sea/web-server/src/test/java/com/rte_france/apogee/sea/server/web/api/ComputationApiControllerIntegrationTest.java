package com.rte_france.apogee.sea.server.web.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.model.dao.computation.*;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.SnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotRepository;
import com.rte_france.apogee.sea.server.services.computation.ComputationService;
import com.rte_france.apogee.sea.server.services.utility.TimerangeFilter;
import com.rte_france.apogee.sea.server.third.IThirdService;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@AutoConfigureDataJpa
@EntityScan("com.rte_france.apogee.sea.server")
@EnableJpaRepositories("com.rte_france.apogee.sea.server")
@ContextConfiguration(classes = {ComputationApiController.class, ComputationService.class, NetworkContextRepository.class, ComputationResultRepository.class,
        NetworkPostContingencyResultRepository.class, CaseCategoryRepository.class, CaseTypeRepository.class, UiSnapshotRepository.class, SnapshotRepository.class,
        UiSnapshotDaoImpl.class, NetworkLimitViolationsResultRepository.class, NetworkRepository.class, TimerangeFilter.class})
@WebMvcTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComputationApiControllerIntegrationTest {

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ObjectMapper objectMapper;

    @MockBean
    private IThirdService iThirdService;

    @Test
    @Sql({"/test-context-data.sql"})
    public void getSAResultsTest() {
        String instantExpected = "2018-10-16T09:00:00Z";
        Clock clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.systemDefault());
        Instant instant = Instant.now(clock);

        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };

        Instant now = Instant.now();

        assertThat(now.toString()).isEqualTo(instantExpected);

        try {
            mvc.perform(get("/computation/context/lastwithpriority")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.length()").value(30))
                    .andExpect(jsonPath("$[?(@.caseType.name == 'pf')]", hasSize(6)))
                    .andExpect(jsonPath("$[?(@.caseType.name == 'srmixte')]", hasSize(3)))
                    .andExpect(jsonPath("$[?(@.caseType.name == 'srj-ij')]", hasSize(21)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
