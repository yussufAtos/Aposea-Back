package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.AbstractComputationResult;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbstractComputationResultRepository extends JpaRepository<AbstractComputationResult, Long> {
    List<AbstractComputationResult> findByNetworkContext(NetworkContext networkContext);
}
