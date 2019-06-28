package com.rte_france.apogee.sea.server.model.dao.user;

import com.rte_france.apogee.sea.server.model.user.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    Optional<Authority> findOneByName(String name);
}
