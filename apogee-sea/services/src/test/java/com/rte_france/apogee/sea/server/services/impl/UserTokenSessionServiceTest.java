package com.rte_france.apogee.sea.server.services.impl;

import com.rte_france.apogee.sea.server.model.dao.user.UserTokenSessionRepository;
import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.any;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest(classes = UserDetailsService.class)
public class UserTokenSessionServiceTest {

    private UserTokenSessionRepository userTokenSessionRepository;

    @InjectMocks
    private UserTokenSessionServiceImpl userTokenSessionService;

    private UserTokenSession userTokenSession;

    @BeforeEach
    public void setup() throws IOException {

        MockitoAnnotations.initMocks(this);
        userTokenSessionRepository = Mockito.mock(UserTokenSessionRepository.class);
        Field field = ReflectionUtils.findField(UserTokenSessionServiceImpl.class, "userTokenSessionRepository");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, userTokenSessionService, userTokenSessionRepository);

        String username = "user";

        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        userTokenSession = new UserTokenSession(username, httpServletRequest.getHeader("Authorization"), "JSESSION : Test-123", Long.valueOf(3600));
    }

    //    @Test(expected = UsernameNotFoundException.class)
    public void testIsValidUserTokenSessionMapping() {

        UserTokenSession mockUserTokenSession = Mockito.spy(userTokenSession);
        Mockito.when(userTokenSessionRepository.findOneByUsername(userTokenSession.getUsername())).thenReturn(Optional.of(mockUserTokenSession));
        Mockito.when(mockUserTokenSession.getCreatedTime()).thenReturn(Instant.now().plus(2, ChronoUnit.DAYS));

        userTokenSessionService.isValidUserTokenSessionMapping(userTokenSession);

    }

    @Test
    public void testSaveUserTokenSessionMapping() {

        Mockito.when(userTokenSessionRepository.save(any(UserTokenSession.class))).thenReturn(userTokenSession);
        assertEquals(userTokenSessionService.saveUserTokenSessionMapping(userTokenSession), userTokenSession);
    }

}
