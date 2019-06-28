package com.rte_france.apogee.sea.server.model.dao.computation;


import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkPostContingencyResult;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkPostContingencyResultRepository extends JpaRepository<NetworkPostContingencyResult, Long> {

    @Query(value = "SELECT n FROM NetworkPostContingencyResult n WHERE n.networkContingency.id = :networkContingencyId and n.computationResult.id =:computationResultId")
    Optional<NetworkPostContingencyResult> findByNetworkContingencyAndComputationResult(@Param("networkContingencyId") String networkContingencyId,
                                                                                        @Param("computationResultId") Long computationResultId);

    @Query(value = "SELECT n FROM NetworkPostContingencyResult n WHERE n.computationResult.id =:computationResultId")
    List<NetworkPostContingencyResult> findByComputationResult(@Param("computationResultId") Long computationResultId);

    @Query(value = "SELECT p FROM NetworkPostContingencyResult p JOIN p.computationResult sa " +
            " WHERE sa.networkContext = :networkContext AND p.networkContingency=:networkContingency")
    Optional<NetworkPostContingencyResult> findByNetworkContextAndNetworkContingency(@Param("networkContext") NetworkContext networkContext, @Param("networkContingency") NetworkContingency networkContingency);


    @Query(value = "SELECT n FROM NetworkPostContingencyResult n WHERE n.computationResult.networkContext=:networkContext")
    List<NetworkPostContingencyResult> findPostByNetworkContext(@Param("networkContext") NetworkContext networkContext);

    @Query(value = "SELECT r FROM NetworkPostContingencyResult p JOIN p.remedials r JOIN p.computationResult sa " +
            " WHERE sa.networkContext.id = :networkContextId AND p.networkContingency.id=:networkContingencyId")
    List<Remedial> findRemedialsByNetworkContextAndNetworkContingency(@Param("networkContextId") Long networkContextId, @Param("networkContingencyId") String networkContingencyId);

    @Query(value = "SELECT p FROM NetworkPostContingencyResult p JOIN p.remedials r WHERE r.idRemedialRepository=:remedialId")
    List<NetworkPostContingencyResult> findAllPostContingencyResultByRemedialId(@Param("remedialId") String remedialId);
}
