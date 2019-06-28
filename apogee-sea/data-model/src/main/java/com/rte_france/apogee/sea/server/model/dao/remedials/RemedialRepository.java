package com.rte_france.apogee.sea.server.model.dao.remedials;

import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RemedialRepository extends JpaRepository<Remedial, String> {
}


