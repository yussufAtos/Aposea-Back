package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkLimitViolationRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkBaseVoltageRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkElementRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkVoltageLevelRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.zones.NetworkBaseVoltage;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
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

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {ZonesApiController.class})
@WebMvcTest
@ActiveProfiles("test")
public class ZonesApiControllerTest {

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkBaseVoltageRepository networkBaseVoltageRepository;

    @MockBean
    private NetworkVoltageLevelRepository networkVoltageLevelRepository;

    @MockBean
    private NetworkZoneRepository networkZoneRepository;

    @MockBean
    private NetworkElementRepository networkElementRepository;

    @MockBean
    private NetworkLimitViolationRepository networkLimitViolationRepository;

    private NetworkBaseVoltage networkBaseVoltage = new NetworkBaseVoltage("1", "test", 2.0);

    private NetworkVoltageLevel networkVoltageLevel = new NetworkVoltageLevel("1", "test", networkBaseVoltage);

    private NetworkZone networkZone = new NetworkZone("1", "test", new HashSet<>(Arrays.asList(networkVoltageLevel)));

    @Test
    public void getNetworkBaseVoltagesTest() {
        List<NetworkBaseVoltage> networkBaseVoltages = new ArrayList<>();

        when(networkBaseVoltageRepository.findAll())
                .thenReturn(networkBaseVoltages);
        try {
            mvc.perform(get("/zones/networkBaseVoltages")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkBaseVoltageWithResultOkTest() {
        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkBaseVoltage));

        try {
            mvc.perform(get("/zones/networkBaseVoltages/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkBaseVoltageWithResultNotFoundTest() {
        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/zones/networkBaseVoltages/1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteNetworkBaseVoltageWithResultOkTest() {

        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkBaseVoltage));

        try {
            mvc.perform(delete("/zones/networkBaseVoltages/{objectid}", "1")
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
    public void deleteNetworkBaseVoltageWithResultNotFoundTest() {
        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/zones/networkBaseVoltages/{objectid}", "1")
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
    public void addNetworkBaseVoltageTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkBaseVoltageRepository.save(networkBaseVoltage))
                .thenReturn(networkBaseVoltage);

        try {
            mvc.perform(post("/zones/networkBaseVoltages")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Arrays.asList(networkBaseVoltage)))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkBaseVoltageWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkBaseVoltage));

        try {
            mvc.perform(put("/zones/networkBaseVoltages/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkBaseVoltage))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkBaseVoltageWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkBaseVoltageRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/zones/networkBaseVoltages/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkBaseVoltage))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void getNetworkVoltageLevelsTest() {
        List<NetworkVoltageLevel> networkVoltageLevels = new ArrayList<>();

        when(networkVoltageLevelRepository.findAll())
                .thenReturn(networkVoltageLevels);
        try {
            mvc.perform(get("/zones/networkVoltageLevels")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkVoltageLevelWithResultOkTest() {
        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(get("/zones/networkVoltageLevels/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkVoltageLevelWithResultNotFoundTest() {
        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/zones/networkVoltageLevels/1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteNetworkVoltageLevelWithResultOkTest() {

        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(delete("/zones/networkVoltageLevels/{objectid}", "1")
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
    public void deleteNetworkVoltageLevelWithResultNotFoundTest() {
        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/zones/networkVoltageLevels/{objectid}", "1")
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
    public void addNetworkVoltageLevelTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkVoltageLevelRepository.save(networkVoltageLevel))
                .thenReturn(networkVoltageLevel);

        when(networkBaseVoltageRepository.findByObjectid(networkBaseVoltage.getObjectid()))
                .thenReturn(Optional.of(networkBaseVoltage));

        try {
            mvc.perform(post("/zones/networkVoltageLevels")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Arrays.asList(networkVoltageLevel)))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkVoltageLevelWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkVoltageLevel));

        when(networkBaseVoltageRepository.findByObjectid(networkBaseVoltage.getObjectid()))
                .thenReturn(Optional.of(networkBaseVoltage));

        try {
            mvc.perform(put("/zones/networkVoltageLevels/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkVoltageLevel))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkVoltageLevelWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkVoltageLevelRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/zones/networkVoltageLevels/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkVoltageLevel))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZonesTest() {
        List<NetworkZone> networkZones = new ArrayList<>();

        when(networkZoneRepository.findAll())
                .thenReturn(networkZones);
        try {
            mvc.perform(get("/zones/networkZones")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZoneWithResultOkTest() {
        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkZone));

        try {
            mvc.perform(get("/zones/networkZones/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZoneWithResultNotFoundTest() {
        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/zones/networkZones/1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteNetworkZoneWithResultOkTest() {

        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkZone));

        try {
            mvc.perform(delete("/zones/networkZones/{objectid}", "1")
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
    public void deleteNetworkZoneWithResultNotFoundTest() {
        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/zones/networkZones/{objectid}", "1")
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
    public void addNetworkZoneTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkZoneRepository.save(networkZone))
                .thenReturn(networkZone);

        when(networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(post("/zones/networkZones")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Arrays.asList(networkZone)))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkZoneWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.of(networkZone));

        when(networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(put("/zones/networkZones/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkZone))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateNetworkZoneWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkZoneRepository.findByObjectid("1"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/zones/networkZones/{objectid}", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkZone))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addNetworkVoltageLevelsByNetworkZoneTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkZoneRepository.findByObjectid(networkZone.getObjectid()))
                .thenReturn(Optional.of(networkZone));

        when(networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(post("/zones/networkZones/{objectid}/networkVoltageLevels", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Arrays.asList(networkVoltageLevel)))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteNetworkVoltageLevelsByNetworkZoneTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkZoneRepository.findByObjectid(networkZone.getObjectid()))
                .thenReturn(Optional.of(networkZone));

        when(networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()))
                .thenReturn(Optional.of(networkVoltageLevel));

        try {
            mvc.perform(delete("/zones/networkZones/{objectid}/networkVoltageLevels", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Arrays.asList(networkVoltageLevel)))
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZonesByNetworkVoltageLevelTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(networkVoltageLevelRepository.findByObjectid(networkVoltageLevel.getObjectid()))
                .thenReturn(Optional.of(networkVoltageLevel));

        when(networkZoneRepository.findByNetworkVoltageLevels_Objectid(networkVoltageLevel.getObjectid()))
                .thenReturn(new HashSet<>(Arrays.asList(networkZone)));

        try {
            mvc.perform(get("/zones/networkVoltageLevels/{objectid}/networkZones", "1")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
