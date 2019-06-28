package com.rte_france.apogee.sea.server.services.impl;

import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.user.User;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;

import java.util.Optional;

@Service("userDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {

        Optional<User> userFromDataBase = userRepository.findOneByUsername(username);

        if (!userFromDataBase.isPresent()) {
            LOGGER.info("User {} was not found in the database", username);
            throw new UsernameNotFoundException("User " + username + " was not found in the database");
        }

        return userFromDataBase.get();

    }
}
