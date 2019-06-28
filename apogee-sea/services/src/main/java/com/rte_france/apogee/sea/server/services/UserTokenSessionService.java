package com.rte_france.apogee.sea.server.services;

import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.userdetails.UsernameNotFoundException;


public interface UserTokenSessionService {

    /**
     * Check if there is mapping between oauth token, username and session-id.
     * And the token is not yet expired.
     * @param userTokenSession
     * @return ValidMappingResponse if valid mapping else throw {@link UsernameNotFoundException}
     */
    ValidMappingResponse isValidUserTokenSessionMapping(UserTokenSession userTokenSession);

    /**
     *
     * @param userTokenSession
     * @return token session record from data base.
     */
    UserTokenSession saveUserTokenSessionMapping(UserTokenSession userTokenSession);


    /**
     * Class to store isValidUserTokenSessionMapping() response.
     */
    @AllArgsConstructor
    @NoArgsConstructor
    class ValidMappingResponse {

        @Getter
        @Setter
        private boolean valid;

        @Getter
        @Setter
        private UserTokenSession userTokenSession;
    }

}
