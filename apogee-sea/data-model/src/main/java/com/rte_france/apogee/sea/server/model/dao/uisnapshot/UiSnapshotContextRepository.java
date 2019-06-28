package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UiSnapshotContextRepository extends JpaRepository<UiSnapshotContext, Long> {
    @Query(value = "SELECT uc.networkContext FROM  UiSnapshotContext uc WHERE uc.uiSnapshot.id =:uiSnapshotId ORDER BY uc.networkContext.networkDate")
    List<NetworkContext> findLatestContextByUiSnapshotId(@Param("uiSnapshotId") Long uiSnapshotId);

    @Query(value = "SELECT uc.networkContext FROM  UiSnapshotContext uc WHERE uc.uiSnapshot.id =:uiSnapshotId AND uc.networkContext.networkDate >=:startTime AND uc.networkContext.networkDate<=:endTime " +
            "ORDER BY uc.networkContext.networkDate")
    List<NetworkContext> findLatestContextByUiSnapshotIdAndFilterByTimerange(@Param("uiSnapshotId") Long uiSnapshotId, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query(value = "SELECT uc FROM  UiSnapshotContext uc WHERE uc.uiSnapshot.id =:uiSnapshotId")
    List<UiSnapshotContext> findByUiSnapshotId(@Param("uiSnapshotId") Long uiSnapshotId);


    @Query(value = "SELECT DISTINCT  uc.networkContext FROM  UiSnapshotContext uc WHERE uc.networkContext.id IN :networkcontextsIds ORDER BY uc.networkContext.networkDate")
    List<NetworkContext> findNetworkcontextByIds(@Param("networkcontextsIds") List<Long> networkcontextsIds);
}


