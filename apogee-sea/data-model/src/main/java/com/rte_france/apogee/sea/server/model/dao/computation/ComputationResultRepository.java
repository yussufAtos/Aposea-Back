package com.rte_france.apogee.sea.server.model.dao.computation;


import com.rte_france.apogee.sea.server.model.computation.AbstractComputationResult;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComputationResultRepository extends JpaRepository<AbstractComputationResult, Long> {
    public Optional<AbstractComputationResult> findByIdAfsRunner(String idAfsRunner);
    List<AbstractComputationResult> findByNetworkContext(NetworkContext networkContext);
}
