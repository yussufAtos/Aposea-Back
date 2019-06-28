package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UiSnapshotRepository extends JpaRepository<UiSnapshot, Long> {

    @Query(value = "SELECT u FROM UiSnapshot u WHERE u.createdDate = (SELECT MAX(ui.createdDate) FROM UiSnapshot ui)")
    Optional<UiSnapshot> findLatestUiSnapshot();

    @Query(value = "SELECT u.id FROM UiSnapshot u WHERE u.createdDate = (SELECT MIN(ui.createdDate) FROM UiSnapshot ui)")
    Long findOldesestId();
}

