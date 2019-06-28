package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.ComputationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComputationDataRepository extends JpaRepository<ComputationData, String> {
    public Optional<ComputationData> findByIdAfsSecurityAnalysisRunner(String idAfsSecurityAnalysisRunner);
}
