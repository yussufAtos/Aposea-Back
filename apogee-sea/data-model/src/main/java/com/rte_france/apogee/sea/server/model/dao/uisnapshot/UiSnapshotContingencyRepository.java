package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;


@Repository
public interface UiSnapshotContingencyRepository extends JpaRepository<UiSnapshotContingency, Long> {
    // objectids : object ids of zones
    @Query(value = "SELECT DISTINCT uc.networkContingency.id FROM UiSnapshotContingency uc JOIN uc.networkZones z " +
            "WHERE uc.uiSnapshot.id=:uiSnapshotId AND z.objectid IN (:objectids) order by uc.networkContingency.id ASC")
    Page<String> findContingencies(@Param("objectids") List<String> objectids, @Param("uiSnapshotId") Long uiSnapshotId, Pageable pageable);

    // objectids : object ids of zones
    //excludeContingencies : contingency to exclude
    @Query(value = "SELECT DISTINCT uc FROM UiSnapshotContingency uc JOIN uc.networkZones z " +
            "WHERE uc.uiSnapshot.id=:uiSnapshotId AND z.objectid IN (:objectids) AND uc.networkContingency NOT IN (:excludeContingencies) order by uc.networkContingency.id ASC")
    Page<UiSnapshotContingency> findUiSnapshotContingenciesWithZonesAndSnapshotIdAndPageableAndExcludeZones(@Param("objectids") List<String> objectids, @Param("uiSnapshotId") Long uiSnapshotId, Pageable pageable, @Param("excludeContingencies") List<NetworkContingency> excludeContingencies);


    @Query(value = "SELECT DISTINCT uc FROM UiSnapshotContingency uc JOIN uc.networkZones z  JOIN uc.uiSnapshotContingencyContextList ncc JOIN ncc.uiSnapshotContext sc " +
            "WHERE uc.uiSnapshot.id=:uiSnapshotId AND z.objectid IN (:objectids) AND sc.networkContext.networkDate >=:startTime AND sc.networkContext.networkDate<=:endTime AND ncc.status <> 'NO_V'  " +
            "AND uc.networkContingency NOT IN (:excludeContingencies) order by uc.networkContingency.id ASC")
    Page<UiSnapshotContingency> findUiSnapshotContingenciesWithZonesAndTimerangeAndSnapshotIdAndPageableAndExcludeZones(@Param("objectids") List<String> objectids, @Param("uiSnapshotId") Long uiSnapshotId, Pageable pageable, @Param("startTime") Instant startTime,
                                                                                                                        @Param("endTime") Instant endTime, @Param("excludeContingencies") List<NetworkContingency> excludeContingencies);

    @Query(value = "SELECT DISTINCT uc FROM UiSnapshotContingency uc JOIN uc.networkZones z  JOIN uc.uiSnapshotContingencyContextList ncc JOIN ncc.uiSnapshotContext sc " +
            "WHERE uc.uiSnapshot.id=:uiSnapshotId AND z.objectid IN (:objectids) AND sc.networkContext.networkDate >=:startTime AND sc.networkContext.networkDate<=:endTime AND ncc.status <> 'NO_V'  order by uc.networkContingency.id ASC")
    List<UiSnapshotContingency> findUiSnapshotContingenciesWithZonesAndTimerangeAndSnapshotId(@Param("objectids") List<String> objectids, @Param("uiSnapshotId") Long uiSnapshotId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query(value = "SELECT DISTINCT uc FROM UiSnapshotContingency uc JOIN uc.networkZones z  JOIN uc.uiSnapshotContingencyContextList ncc JOIN ncc.uiSnapshotContext sc " +
            "WHERE uc.uiSnapshot.id=:uiSnapshotId AND z.objectid IN (:objectids) AND ncc.status <> 'NO_V'  order by uc.networkContingency.id ASC")
    List<UiSnapshotContingency> findUiSnapshotContingenciesWithZonesAndSnapshotId(@Param("objectids") List<String> objectids, @Param("uiSnapshotId") Long uiSnapshotId);

    @Query(value = "SELECT uc FROM UiSnapshotContingency uc WHERE uc.networkContingency.id=:networkContingencyId")
    List<UiSnapshotContingency> findUiSnapshotContingencies(@Param("networkContingencyId") String networkContingencyId);

}
