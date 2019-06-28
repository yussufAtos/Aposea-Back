package com.rte_france.apogee.sea.server.services.impl;


import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.user.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@SpringBootTest(classes = UserDetailsService.class)
public class UserDetailsServiceTest {

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private UserRepository userRepository;

    @Value("${config.oauth2.tokenTimeout}")
    private String tokenExpiryTime;

    private User user;

    @BeforeEach
    public void setup() {

        MockitoAnnotations.initMocks(this);
        userRepository = Mockito.mock(UserRepository.class);

        Field field = ReflectionUtils.findField(UserDetailsServiceImpl.class, "userRepository");
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, userDetailsService, userRepository);

        user = Mockito.mock(User.class);
    }

    @Test
    public void testLoadUserByUsername() {

        String username = "user";
        Mockito.when(userRepository.findOneByUsername(username)).thenReturn(Optional.of(user));

        Assertions.assertEquals(userDetailsService.loadUserByUsername(username), user);
    }
}
