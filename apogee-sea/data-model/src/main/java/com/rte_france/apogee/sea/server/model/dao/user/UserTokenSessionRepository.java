package com.rte_france.apogee.sea.server.model.dao.user;

import com.rte_france.apogee.sea.server.model.user.UserTokenSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserTokenSessionRepository extends CrudRepository<UserTokenSession, Long> {

    Optional<UserTokenSession> findOneByUsername(String username);
}
