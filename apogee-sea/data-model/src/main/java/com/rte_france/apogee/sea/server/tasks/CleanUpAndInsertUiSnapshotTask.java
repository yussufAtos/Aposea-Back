package com.rte_france.apogee.sea.server.tasks;

import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotRepository;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
public class CleanUpAndInsertUiSnapshotTask {
    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    private UiSnapshotRepository uiSnapshotRepository;

    @Autowired
    public CleanUpAndInsertUiSnapshotTask(UiSnapshotDaoImpl uiSnapshotDaoImpl, UiSnapshotRepository uiSnapshotRepository) {
        this.uiSnapshotDaoImpl = uiSnapshotDaoImpl;
        this.uiSnapshotRepository = uiSnapshotRepository;
    }

    @Getter
    @Value("${apogee.uisnapshot.rateUisnapshotConvergenceFailed:900000}")
    private String rateUisnapshotConvergenceFailed;

    @Scheduled(initialDelayString = "${apogee.uisnapshot.cleanup.initialDelay}",
            fixedRateString = "${apogee.uisnapshot.cleanup.fixedRate}")
    public void run() {
        long convergenceFailed = Long.parseLong(rateUisnapshotConvergenceFailed);
        Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotRepository.findLatestUiSnapshot();
        Instant dateNow = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant dateUisnapshot = null;
        if (uiSnapshotOptional.isPresent()) {
            dateUisnapshot = uiSnapshotOptional.get().getCreatedDate();
            long creationAge = dateNow.toEpochMilli() - dateUisnapshot.toEpochMilli();
            if (creationAge > convergenceFailed) {
                uiSnapshotDaoImpl.handleUiSnapshotCreation();
            }
        } else {
            uiSnapshotDaoImpl.handleUiSnapshotCreation();
        }
    }
}
