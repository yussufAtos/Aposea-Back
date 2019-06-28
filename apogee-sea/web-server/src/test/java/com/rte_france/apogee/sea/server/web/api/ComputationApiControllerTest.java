package com.rte_france.apogee.sea.server.web.api;

import com.rte_france.apogee.sea.server.model.computation.AbstractComputationResult;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkSecurityAnalysisResult;
import com.rte_france.apogee.sea.server.model.dao.computation.*;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.*;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.services.computation.IComputationService;
import com.rte_france.apogee.sea.server.third.IThirdService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {ComputationApiController.class})
@WebMvcTest
@ActiveProfiles("test")
public class ComputationApiControllerTest {

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkContextRepository networkContextRepository;

    @MockBean
    private ComputationResultRepository computationResultRepository;

    @MockBean
    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    @MockBean
    private NetworkLimitViolationsResultRepository networkLimitViolationsResultRepository;

    @MockBean
    private CaseCategoryRepository caseCategoryRepository;

    @MockBean
    private CaseTypeRepository caseTypeRepository;

    @MockBean
    private IComputationService IComputationService;

    @MockBean
    private IThirdService iThirdService;

    @MockBean
    private UiSnapshotRepository uiSnapshotRepository;

    @MockBean
    private UiSnapshotContingencyRepository uiSnapshotContingencyRepository;

    @MockBean
    private UiSnapshotContextRepository uiSnapshotContextRepository;

    @MockBean
    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;

    @MockBean
    UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @MockBean
    private UserRepository userRepository;

    @Test
    public void getNetworkContextsTest() {
        List<NetworkContext> networkContexts = new ArrayList<>();

        when(networkContextRepository.findAll())
                .thenReturn(networkContexts);
        try {
            mvc.perform(get("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            mvc.perform(get("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("type", "")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(networkContextRepository.findByCaseType("pf"))
                .thenReturn(networkContexts);
        try {
            mvc.perform(get("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("type", "pf")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getLastNetworkContextsTest() {
        List<NetworkContext> networkContexts = new ArrayList<>();
        when(networkContextRepository.findLatestByCaseType("pf"))
                .thenReturn(networkContexts);

        try {
            mvc.perform(get("/computation/context/last")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("type", "pf")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(networkContextRepository.findLatestNetworkContexts())
                .thenReturn(networkContexts);
        try {
            mvc.perform(get("/computation/context/last")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("type", "")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try {
            mvc.perform(get("/computation/context/last")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getComputationResultTest() {
        AbstractComputationResult computationResult = new NetworkSecurityAnalysisResult();
        when(computationResultRepository.findByIdAfsRunner("GKJHLKHLKDJLKJJLJLJ"))
                .thenReturn(Optional.of(computationResult));
        try {
            mvc.perform(get("/computation/result/{idAfsRunner}", "GKJHLKHLKDJLKJJLJLJ")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteNetworkContextsTest() {

        try {
            mvc.perform(delete("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            mvc.perform(delete("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("idAfsImportedCase", "")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        NetworkContext networkContext = new NetworkContext();
        when(networkContextRepository.findByIdAfsImportedCase("idAfsImportedCase"))
                .thenReturn(Optional.of(networkContext));
        try {
            mvc.perform(delete("/computation/context")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .param("idAfsImportedCase", "idAfsImportedCase")
                    .accept(MediaType.TEXT_PLAIN))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
