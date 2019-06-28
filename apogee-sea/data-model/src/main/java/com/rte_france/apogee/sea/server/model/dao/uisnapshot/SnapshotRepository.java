package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Getter
public class SnapshotRepository {

    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;

    private UiSnapshotContingencyRepository uiSnapshotContingencyRepository;

    private UiSnapshotContextRepository uiSnapshotContextRepository;

    @Autowired
    public SnapshotRepository(UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository, UiSnapshotContingencyRepository uiSnapshotContingencyRepository,
                              UiSnapshotContextRepository uiSnapshotContextRepository) {
        this.uiSnapshotContingencyContextRepository = uiSnapshotContingencyContextRepository;
        this.uiSnapshotContingencyRepository = uiSnapshotContingencyRepository;
        this.uiSnapshotContextRepository = uiSnapshotContextRepository;

    }

}
