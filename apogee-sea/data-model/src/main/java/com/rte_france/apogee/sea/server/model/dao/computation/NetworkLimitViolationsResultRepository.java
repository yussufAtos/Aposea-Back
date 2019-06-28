package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolationsResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NetworkLimitViolationsResultRepository extends JpaRepository<NetworkLimitViolationsResult, Long> {

    @Query(value = "SELECT n FROM NetworkLimitViolationsResult n WHERE n.networkSecurityAnalysisResultList.networkContext=:networkContext")
    List<NetworkLimitViolationsResult> findLimitViolationsResultByNetworkContext(@Param("networkContext") NetworkContext networkContext);
}
