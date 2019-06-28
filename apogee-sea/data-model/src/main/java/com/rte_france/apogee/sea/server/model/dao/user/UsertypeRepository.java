package com.rte_france.apogee.sea.server.model.dao.user;

import com.rte_france.apogee.sea.server.model.user.Usertype;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsertypeRepository extends JpaRepository<Usertype, Long> {
    Optional<Usertype> findByName(String name);
}

