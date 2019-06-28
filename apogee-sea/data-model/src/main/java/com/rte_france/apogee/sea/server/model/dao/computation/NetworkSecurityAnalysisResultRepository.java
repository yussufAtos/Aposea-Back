package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.NetworkSecurityAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NetworkSecurityAnalysisResultRepository extends JpaRepository<NetworkSecurityAnalysisResult, Long> {
    public Optional<NetworkSecurityAnalysisResult> findByIdAfsRunner(String idAfsRunner);
}
