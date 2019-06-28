package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.uisnapshot.Status;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingencyContext;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UiSnapshotContingencyContextRepository extends JpaRepository<UiSnapshotContingencyContext, Long> {

    @Query(value = "SELECT ucc FROM UiSnapshotContingencyContext ucc JOIN ucc.uiSnapshotContingency uc JOIN uc.uiSnapshot u " +
            "WHERE uc.networkContingency.id=:contingencyId AND u.id=:uiSnapshotId ORDER BY ucc.uiSnapshotContext.networkContext.networkDate")
    List<UiSnapshotContingencyContext> findUiSnapshotContingencyContextByContingency(@Param("contingencyId") String contingencyId, @Param("uiSnapshotId") Long uiSnapshotId);


    @Query(value = "SELECT ucc FROM UiSnapshotContingencyContext ucc JOIN ucc.uiSnapshotContext uct JOIN ucc.uiSnapshotContingency uc JOIN uc.uiSnapshot u " +
            "WHERE ucc.status IN :statusList AND uct.networkContext IN :networkContexts AND :zone  MEMBER OF uc.networkZones AND u.id=:uiSnapshotId ORDER BY ucc.uiSnapshotContingency.networkContingency.id")
    List<UiSnapshotContingencyContext> findAllByNetworkContextsAndNetworkZonesAnAndStatusList(@Param("networkContexts") List<NetworkContext> networkContexts, @Param("zone") NetworkZone zone, @Param("statusList") List<Status> statusList, @Param("uiSnapshotId") Long uiSnapshotId);

    @Modifying
    @Query(value = "DELETE FROM UiSnapshotContingencyContext  ucc WHERE ucc.uiSnapshotContext.id IN (SELECT uc.id FROM UiSnapshotContext uc WHERE uc.networkContext.id" +
            " =(SELECT nc.id FROM NetworkContext nc WHERE nc.id=:networkContextId))")
    int deleteUiSnapshotContingencyContextByNetworkContext(@Param("networkContextId") Long networkContextId);

    @Modifying
    @Query(value = "DELETE FROM UiSnapshotContext uc WHERE uc.networkContext.id" +
            " =(SELECT nc.id FROM NetworkContext nc WHERE nc.id=:networkContextId)")
    int deleteUiSnapshotContextByNetworkContext(@Param("networkContextId") Long networkContextId);

}
