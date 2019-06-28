package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolation;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface NetworkLimitViolationRepository extends JpaRepository<NetworkLimitViolation, Long> {

    @Query(value = "select lm from NetworkLimitViolation lm join lm.networkLimitViolationsResult lmr JOIN lm.networkVoltageLevels nvl " +
            "join lmr.networkPostContingencyResult pcr join pcr.computationResult cr " +
            "where cr.networkContext=:networkContext and pcr.networkContingency=:networkContingency AND nvl.objectid NOT IN (:voltageLevelsToExclude)")
    List<NetworkLimitViolation> findAllByNetworkContextByNetworkContingencyAndZoneExclusion(@Param("networkContext") NetworkContext networkContext, @Param("networkContingency") NetworkContingency networkContingency, @Param("voltageLevelsToExclude") List<String> voltageLevelsToExclude);


    @Query(value = "SELECT lmv FROM NetworkLimitViolation lmv JOIN lmv.networkLimitViolationsResult lvr JOIN lmv.networkVoltageLevels nvl JOIN nvl.networkZones z " +
            "WHERE lvr.networkSecurityAnalysisResultList.networkContext=:networkContext and " +
            "z.objectid IN (:objectidsZones) AND nvl.objectid NOT IN (:voltageLevelsToExclude)")
    List<NetworkLimitViolation> findLimitViolationsResultByNetworkContextAndZonesAndZoneExclusion(@Param("networkContext") NetworkContext networkContext, @Param("objectidsZones") List<String> objectidsZones, @Param("voltageLevelsToExclude") List<String> voltageLevelsToExclude);

    @Query(value = "SELECT lmv FROM NetworkLimitViolation lmv JOIN lmv.networkLimitViolationsResult lvr JOIN lmv.networkVoltageLevels nvl JOIN nvl.networkZones z " +
            "WHERE lvr.networkSecurityAnalysisResultList.networkContext=:networkContext and " +
            "z.objectid IN (:objectidsZones)")
    List<NetworkLimitViolation> findLimitViolationsResultByNetworkContextAndZones(@Param("networkContext") NetworkContext networkContext, @Param("objectidsZones") List<String> objectidsZones);


    @Query(value = "select lm.networkVoltageLevels from NetworkLimitViolation lm join lm.networkLimitViolationsResult lmr " +
            "join lmr.networkPostContingencyResult pcr join pcr.computationResult cr " +
            "where cr.networkContext.id=:networkContextId and pcr.networkContingency.id=:contingencyId")
    Set<NetworkVoltageLevel> findVoltageLevelsByNetworkContextAndNetworkContingency(@Param("networkContextId") Long networkContextId, @Param("contingencyId") String contingencyId);
}
