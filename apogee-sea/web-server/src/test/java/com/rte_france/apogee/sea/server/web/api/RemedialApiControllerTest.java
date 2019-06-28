package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.powsybl.contingency.ContingencyElementType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingencyElement;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedial;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.prioritize.IPrioritizeRemedialsService;
import com.rte_france.apogee.sea.server.services.prioritize.PrioritizeRemedialService;
import com.rte_france.apogee.sea.server.third.IThirdService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {RemedialApiController.class})
@WebMvcTest
@ActiveProfiles("test")
public class RemedialApiControllerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialApiControllerTest.class);

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PrioritizeRemedialService prioritizeRemedialService;

    @MockBean
    private IThirdService iThirdService;

    @Autowired
    private WebApplicationContext context;

    @Test
    public void fetchPrioritizeRemedialsTest() {
        List<Prioritize> prioritizeRemedials = new ArrayList<>();

        try {
            when(prioritizeRemedialService.getPrioritizeRemedial(null, "contingency"))
                    .thenReturn(prioritizeRemedials);
        } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
            LOGGER.error("Error while fetch the prioritize remedials", e);
        }
        try {
            mvc.perform(get("/remedial/prioritize")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            mvc.perform(get("/remedial/prioritize")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("prioritizeDate", "")
                    .param("contingencyId", "contingency")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            when(prioritizeRemedialService.getPrioritizeRemedial("2018-11-12T11:23:18.692Z", "contingency"))
                    .thenReturn(prioritizeRemedials);
        } catch (IPrioritizeRemedialsService.PrioritizeRemedialServiceException e) {
            LOGGER.error("Error while fetch the prioritize remedials", e);
        }
        try {
            mvc.perform(get("/remedial/prioritize")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("prioritizeDate", "2018-11-12T11:23:18.692Z")
                    .param("contingencyId", "contingency")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addPrioritizeRemedialTest() throws Exception {
        List<Prioritize> prioritizes = new ArrayList<>();
        NetworkContingency contingency = new NetworkContingency("N-1 Tavel Realtor");
        Remedial remedial1 = new Remedial("remedial3", "remedial3");
        Remedial remedial2 = new Remedial("remedial1", "remedial1");

        PrioritizeRemedial prioritizeRemedial1 = createPrioritizeRemedial(remedial1, 1);
        PrioritizeRemedial prioritizeRemedial2 = createPrioritizeRemedial(remedial2, 2);
        Instant startDate = Instant.parse("2018-11-12T11:23:18.692Z");
        Instant endDate = Instant.parse("2018-11-12T19:23:18.692Z");
        List<PrioritizeRemedial> prioritizeRemedials = new ArrayList<>();
        prioritizeRemedials.add(prioritizeRemedial1);
        prioritizeRemedials.add(prioritizeRemedial2);
        Prioritize prioritize = createPrioritize(startDate, endDate, contingency, prioritizeRemedials);
        prioritizes.add(prioritize);

        ObjectMapper objectMapper = new ObjectMapper().registerModules(new JSR310Module());
        String json = objectMapper.writeValueAsString(prioritizes);

        mvc.perform(post("/remedial/prioritize")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(json)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    private Prioritize createPrioritize(Instant startDate, Instant endDate, NetworkContingency networkContingency, List<PrioritizeRemedial> prioritizeRemedials) {
        Prioritize prioritize = new Prioritize();
        prioritize.setPrioritizeRemedialList(prioritizeRemedials);
        prioritize.setPrioritizeStartDate(startDate);
        prioritize.setPrioritizeEndDate(endDate);
        prioritize.setNetworkContingency(networkContingency);
        prioritizeRemedials.forEach(prioritizeRemedial -> prioritizeRemedial.setPrioritize(prioritize));
        return prioritize;
    }

    private PrioritizeRemedial createPrioritizeRemedial(Remedial remedial, int prioritizeValue) {
        PrioritizeRemedial prioritizeRemedial = new PrioritizeRemedial();
        prioritizeRemedial.setRemedial(remedial);
        prioritizeRemedial.setPrioritizeValue(prioritizeValue);
        return prioritizeRemedial;
    }

    private NetworkContingency createNetworkContingency(String elementId, String contingencyId) {
        NetworkContingency networkContingency = new NetworkContingency(contingencyId);
        NetworkContingencyElement networkContingencyElement = new NetworkContingencyElement(elementId, ContingencyElementType.BRANCH, networkContingency);
        List<NetworkContingencyElement> elements = new ArrayList<>();
        elements.add(networkContingencyElement);
        networkContingency.getNetworkContingencyElementList().addAll(elements);
        return networkContingency;
    }

}
