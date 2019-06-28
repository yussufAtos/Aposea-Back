package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingencyElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkContingencyElementRepository extends JpaRepository<NetworkContingencyElement, Long> {
    @Query(value = "SELECT n.networkContingency.id FROM NetworkContingencyElement n WHERE n.equipmentName =:equipementName")
    Optional<String> getContingencyId(@Param("equipementName") String equipementName);
}
