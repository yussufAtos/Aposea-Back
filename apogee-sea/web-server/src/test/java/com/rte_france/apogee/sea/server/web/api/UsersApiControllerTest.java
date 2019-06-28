package com.rte_france.apogee.sea.server.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rte_france.apogee.sea.server.model.dao.user.AuthorityRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.user.Authority;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import com.rte_france.apogee.sea.server.wrapper.UsertypeWrapper;
import org.junit.jupiter.api.BeforeEach;
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
@ContextConfiguration(classes = {UsersApiController.class})
@WebMvcTest
@ActiveProfiles("test")
public class UsersApiControllerTest {

    private static final String TEST_USER_ID = "test";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UsertypeRepository usertypeRepository;

    @MockBean
    private AuthorityRepository authorityRepository;

    @MockBean
    private NetworkZoneRepository networkZoneRepository;

    private User user = new User("test", "", true, new ArrayList<>());

    private Usertype usertype = new Usertype("test");

    private Authority authority = new Authority("test");

    private NetworkZone networkZone = new NetworkZone("1", "z1", new HashSet<>());

    @BeforeEach
    public void setUp() {
        user.getUsertypes().add(usertype);
        user.setActualUsertype(usertype);
    }

    @Test
    public void getUsersTest() {
        List<User> users = new ArrayList<>();

        when(userRepository.findAll())
                .thenReturn(users);
        try {
            mvc.perform(get("/users")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUserWithResultOkTest() {
        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.of(user));

        try {
            mvc.perform(get("/users/test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUserWithResultNotFoundTest() {
        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(get("/users/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void deleteUserWithResultOkTest() {

        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.of(user));

        try {
            mvc.perform(delete("/users/{username}", "test")
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
    public void deleteUserWithResultNotFoundTest() {
        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(delete("/users/{username}", "test")
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
    public void addUserTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(userRepository.save(user))
                .thenReturn(user);

        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.empty());

        try {
            mvc.perform(post("/users")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(user))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateUserWithResultOkTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.of(user));

        when(authorityRepository.findOneByName(authority.getName()))
                .thenReturn(Optional.of(authority));

        try {
            mvc.perform(put("/users/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(user))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void updateUserWithResultNotFoundTest() {
        ObjectMapper mapper = new ObjectMapper();

        when(userRepository.findOneByUsername("test"))
                .thenReturn(Optional.empty());
        try {
            mvc.perform(put("/users/{username}", "test")
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(user))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.NOT_FOUND.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getNetworkZonesByUserTest() {
        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));
        when(networkZoneRepository.findByUsertypes(usertype))
                .thenReturn(Collections.singleton(networkZone));

        try {
            mvc.perform(get("/users/{username}/networkZones", user.getUsername())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void addUsertypeByUserTest() {
        ObjectMapper mapper = new ObjectMapper();
        UsertypeWrapper usertypeWrapper = new UsertypeWrapper();
        usertypeWrapper.setDefaultUsertype(usertype);
        usertypeWrapper.setUsertypes(Collections.singletonList(usertype));

        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));
        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));
        try {
            mvc.perform(post("/users/{username}/usertypes", user.getUsername())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(usertypeWrapper))
                    .accept(MediaType.APPLICATION_JSON)
                    .with(csrf())
                    .param("action", "signup"))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void getUsertypesByUserTest() {
        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        try {
            mvc.perform(get("/users/{username}/usertypes", user.getUsername())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().is(HttpStatus.OK.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void deleteUsertypeByUserTest() {
        when(userRepository.findOneByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));
        when(usertypeRepository.findByName(usertype.getName()))
                .thenReturn(Optional.of(usertype));

        try {
            mvc.perform(delete("/users/{username}/usertypes/{name}", user.getUsername(), usertype.getName())
                    .with(user(TEST_USER_ID))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .param("action", "signup"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
