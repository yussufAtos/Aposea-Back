package com.rte_france.apogee.sea.server.services.impl;

import com.rte_france.apogee.sea.server.model.dao.user.UserTokenSessionRepository;
import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import com.rte_france.apogee.sea.server.services.UserTokenSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service("userTokenSessionService")
public class UserTokenSessionServiceImpl implements UserTokenSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTokenSessionServiceImpl.class);

    private static final String USER = "User ";

    private UserTokenSessionRepository userTokenSessionRepository;

    @Autowired
    public UserTokenSessionServiceImpl(UserTokenSessionRepository userTokenSessionRepository) {
        this.userTokenSessionRepository = userTokenSessionRepository;
    }

    @Override
    public ValidMappingResponse isValidUserTokenSessionMapping(UserTokenSession userTokenSession) {

        String username = userTokenSession.getUsername();
        Optional<UserTokenSession> userTokenSessionFromDB = userTokenSessionRepository.findOneByUsername(username);

        if (!userTokenSessionFromDB.isPresent()) {
            LOGGER.error("User {} mapping with token is not found in the database.", username);
            throw new UsernameNotFoundException(USER + username + "  mapping with token is not found in the database.");
        }

        Instant currentInstant = Instant.now();

        /*
         * tokenTimeInstant = created_time + expiry time (milliseconds).
         */
        Instant tokenTimeInstant = userTokenSessionFromDB.get().getCreatedTime().plus(userTokenSessionFromDB.get().getExpiryTime(), ChronoUnit.MILLIS);

        if (tokenTimeInstant.isAfter(currentInstant)) {

            LOGGER.info("User {} token has expired. Please generate new token. Deleting the expired token mapping.", username);
            userTokenSessionRepository.delete(userTokenSessionFromDB.get());
            throw new UsernameNotFoundException(USER + username + " token has expired. Please generate new token.");

        } else if (!userTokenSession.equals(userTokenSessionFromDB.get())) {

            if (!userTokenSessionFromDB.get().getToken().equals(userTokenSession.getToken())) {
                LOGGER.info("User {} has invalid user and token mapping. Please generate new token.", userTokenSession.getUsername());
            } else {
                LOGGER.info("User {} has invalid user and session-id mapping. Please generate new token.", userTokenSession.getUsername());
            }

            LOGGER.info("So, Deleting the invalid mapping.");
            userTokenSessionRepository.delete(userTokenSessionFromDB.get());
            throw new UsernameNotFoundException(USER + username + " has invalid user, session-id and token mapping. Please generate new token.");

        } else {

            LOGGER.info("User {} has valid token.", username);
            return new ValidMappingResponse(true, userTokenSessionFromDB.get());
        }

    }

    @Override
    public UserTokenSession saveUserTokenSessionMapping(UserTokenSession userTokenSession) {

        Optional<UserTokenSession> userTokenSessionFromDB = userTokenSessionRepository.findOneByUsername(userTokenSession.getUsername());

        if (userTokenSessionFromDB.isPresent()) {
            if (userTokenSessionFromDB.get().equals(userTokenSession)) {
                LOGGER.info("User {} making login call again with same token and session-id.", userTokenSession.getUsername());
            } else if (!userTokenSessionFromDB.get().getToken().equals(userTokenSession.getToken())) {
                LOGGER.info("User {} making login call with new token", userTokenSession.getUsername());
            } else {
                LOGGER.info("User {} making login call with different session-id", userTokenSession.getUsername());
            }

            LOGGER.info("Deleting older user token session: {}", userTokenSessionFromDB.get());
            userTokenSessionRepository.delete(userTokenSessionFromDB.get());
        }

        return userTokenSessionRepository.save(userTokenSession);
    }


}

