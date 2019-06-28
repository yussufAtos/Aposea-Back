package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.user.Authority;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@ContextConfiguration(classes = {UsertypesApiController.class})
@WebMvcTest
public class UsertypesApiControllerTest {

    private static final String TEST_USER_ID = "user";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UsertypeRepository usertypeRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private NetworkZoneRepository networkZoneRepository;

    private Authority authority = new Authority("test");

    private User user = new User("user", "", true, Collections.singletonList(authority));

    private Usertype usertype = new Usertype("test");

    private NetworkZone networkZone = new NetworkZone("1", "z1", new HashSet<>());

    @Test
    public void getUsertypesTest() throws Exception {
        List<Usertype> usertypes = new ArrayList<>();

        when(usertypeRepository.findAll())
                .thenReturn(usertypes);
        try {
            mvc.perform(get("/usertypes")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUsertypeWithResultOkTest() {
        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.of(usertype));

        try {
            mvc.perform(get("/usertypes/test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUsertypeWithResultNotFoundTest() {
        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/usertypes/{name}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteUsertypeWithResultOkTest() {

        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.of(usertype));

        try {
            mvc.perform(delete("/usertypes/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteUsertypeWithResultNotFoundTest() {
        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/usertypes/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));

            when(userRepository.findUsersByDefaultUsertypeOrActualUsertype("test"))
                    .thenReturn(Collections.singletonList(user));

            mvc.perform(delete("/usertypes/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addUsertypeTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(usertypeRepository.save(usertype))
                .thenReturn(usertype);

        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(post("/usertypes")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(usertype))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateUsertypeWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.of(usertype));

        /*when(authorityRepository.findOneByName(authority.getName()))
                .thenReturn(Optional.of(authority));*/

        try {
            mvc.perform(put("/usertypes/{name}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(usertype))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateUsertypeWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(usertypeRepository.findByName("test"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/usertypes/{name}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(usertype))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void addNetworkZoneByUsertypeTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));
        when(networkZoneRepository.findByObjectid(networkZone.getObjectid()))
                .thenReturn(Optional.of(networkZone));
        when(networkZoneRepository.findByUsertypes(usertype))
                .thenReturn(new HashSet<>(Collections.singletonList(networkZone)));

        try {
            mvc.perform(post("/usertypes/{name}/networkZones", usertype.getName())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(networkZone))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZonesByUsertypeTest() {
        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));
        when(networkZoneRepository.findByUsertypes(usertype))
                .thenReturn(Collections.singleton(networkZone));

        try {
            mvc.perform(get("/usertypes/{name}/networkZones", usertype.getName())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteNetworkZoneByUsertypeTest() {
        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));
        when(networkZoneRepository.findByObjectid(networkZone.getObjectid()))
                .thenReturn(Optional.of(networkZone));
        when(networkZoneRepository.findByUsertypes(usertype))
                .thenReturn(Collections.singleton(networkZone));

        try {
            mvc.perform(delete("/usertypes/{name}/networkZones/{objectid}", usertype.getName(), networkZone.getObjectid())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getActualUsertypeTest() {
        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));

        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        try {
            mvc.perform(get("/usertypes/actualusertypes")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateActualUsertypeTest() {
        Usertype usertypeUpdated = new Usertype("newActual");

        when(usertypeRepository.findByName(usertypeUpdated.getName()))
                .thenReturn(Optional.of(usertypeUpdated));

        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        try {
            mvc.perform(put("/usertypes/actualusertypes/{name}", usertypeUpdated.getName())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
