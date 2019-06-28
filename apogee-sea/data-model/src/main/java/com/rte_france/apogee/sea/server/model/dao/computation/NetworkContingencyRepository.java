package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkContingencyRepository extends JpaRepository<NetworkContingency, String> {
    @Query(value = "SELECT c FROM NetworkContingency c WHERE c.id IN (:ids) ORDER BY c.id ASC")
    List<NetworkContingency> findContingenciesByIds(@Param("ids") List<String> ids);

    @Query(value = "SELECT r FROM NetworkContingency c JOIN c.allRemedials r WHERE c.id=:contingencyId")
    List<Remedial> findAllRemedialsByContingency(@Param("contingencyId") String contingencyId);

    @Query(value = "SELECT c FROM NetworkContingency c JOIN c.allRemedials r WHERE r.idRemedialRepository=:remedialId")
    List<NetworkContingency> findAllContingencyByRemedialId(@Param("remedialId") String remedialId);
}
