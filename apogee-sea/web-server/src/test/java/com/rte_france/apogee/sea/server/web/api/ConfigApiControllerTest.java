package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.logic.TimerangeLogic;
import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import com.rte_france.apogee.sea.server.model.computation.CaseType;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseCategoryRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseTypeRepository;
import com.rte_france.apogee.sea.server.model.dao.timerange.TimerangeTypeRepository;
import com.rte_france.apogee.sea.server.model.timerange.EndType;
import com.rte_france.apogee.sea.server.model.timerange.StartType;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {ConfigApiController.class})
@WebMvcTest
@ActiveProfiles("test")
@WithMockUser
public class ConfigApiControllerTest {

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CaseTypeRepository caseTypeRepository;

    @MockBean
    private CaseCategoryRepository caseCategoryRepository;

    @MockBean
    private TimerangeTypeRepository timerangeTypeRepository;

    private CaseType caseType = new CaseType("1");

    private CaseCategory caseCategory = new CaseCategory("1");

    private TimerangeLogic timerangeLogic = new TimerangeLogic("Sans filtre", "UTC");

    private TimerangeType timerangeType = new TimerangeType("Sans filtre", "UTC", "Sans filtre");

    @BeforeEach
    public void setUp() {
        caseType.setCardTag("1");

        timerangeLogic.setStartType(StartType.NOW);
        timerangeLogic.setEndType(EndType.HOURRELATIVE);
        timerangeLogic.setEndTimeHour(3);
        timerangeLogic.setStartTimeMinutes(15);

        timerangeType.setStartType(StartType.NOW);
        timerangeType.setEndType(EndType.HOURRELATIVE);
        timerangeType.setEndTimeHour(3);
        timerangeType.setStartTimeMinutes(15);
    }

    @Test
    public void getCaseTypesTest() {
        List<CaseType> caseType = new ArrayList<CaseType>();

        when(caseTypeRepository.findAll())
                .thenReturn(caseType);
        try {
            mvc.perform(get("/config/casetypes")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getCaseTypeWithResultOkTest() {
        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.of(caseType));

        try {
            mvc.perform(get("/config/casetypes/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getCaseTypeWithResultNotFoundTest() {
        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/config/casetypes/1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteCaseTypeWithResultOkTest() {

        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.of(caseType));

        try {
            mvc.perform(delete("/config/casetypes/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteCaseTypeWithResultNotFoundTest() {
        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/config/casetypes/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addCaseTypeTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseTypeRepository.save(caseType))
                .thenReturn(caseType);

        try {
            mvc.perform(post("/config/casetypes")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseType))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateCaseTypeWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.of(caseType));

        try {
            mvc.perform(put("/config/casetypes/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseType))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateCaseTypeWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseTypeRepository.findById("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/config/casetypes/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseType))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getCaseCategoriesTest() {
        List<CaseCategory> caseCategory = new ArrayList<CaseCategory>();

        when(caseCategoryRepository.findAll())
                .thenReturn(caseCategory);
        try {
            mvc.perform(get("/config/casecategories")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getCaseCategoryWithResultOkTest() {
        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.of(caseCategory));

        try {
            mvc.perform(get("/config/casecategories/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getCaseCategoryWithResultNotFoundTest() {
        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/config/casecategories/1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteCaseCategoryWithResultOkTest() {

        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.of(caseCategory));

        try {
            mvc.perform(delete("/config/casecategories/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteCaseCategoryWithResultNotFoundTest() {
        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/config/casecategories/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addCaseCategoryTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseCategoryRepository.save(caseCategory))
                .thenReturn(caseCategory);

        try {
            mvc.perform(post("/config/casecategories")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseCategory))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateCaseCategoryWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.of(caseCategory));

        try {
            mvc.perform(put("/config/casecategories/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseCategory))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateCaseCategoryWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(caseCategoryRepository.findById("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/config/casecategories/{uid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(caseCategory))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getTimerangeTypesTest() {
        List<TimerangeType> timerangeTypes = new ArrayList<TimerangeType>();

        when(timerangeTypeRepository.findAll())
                .thenReturn(timerangeTypes);
        try {
            mvc.perform(get("/config/timerangeTypes")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getTimerangeTypeWithResultOkTest() {
        when(timerangeTypeRepository.findById("Sans filtre"))
                .thenReturn(Optional.of(timerangeType));

        try {
            mvc.perform(get("/config/timerangeTypes/{name}", "Sans filtre")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getTimerangeTypeWithResultNotFoundTest() {
        when(timerangeTypeRepository.findById("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/config/timerangeTypes/Sans filtre")
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteTimerangeTypeWithResultOkTest() {

        when(timerangeTypeRepository.findById("Sans filtre"))
                .thenReturn(Optional.of(timerangeType));

        try {
            mvc.perform(delete("/config/timerangeTypes/{name}", "Sans filtre")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteTimerangeTypeWithResultNotFoundTest() {
        when(timerangeTypeRepository.findById("Sans filtre"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/config/timerangeTypes/{name}", "Sans filtre")
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addTimerangeTypeTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(timerangeTypeRepository.save(timerangeType))
                .thenReturn(timerangeType);

        try {
            mvc.perform(post("/config/timerangeTypes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(timerangeLogic))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateTimerangeTypeWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(timerangeTypeRepository.findById("Sans filtre"))
                .thenReturn(Optional.of(timerangeType));

        try {
            mvc.perform(put("/config/timerangeTypes/{name}", "Sans filtre")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(timerangeLogic))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateTimerangeTypeWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(timerangeTypeRepository.findById("Sans filtre"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/config/timerangeTypes/{name}", "Sans filtre")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(timerangeLogic))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
