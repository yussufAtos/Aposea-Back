package com.rte_france.apogee.sea.server.tasks;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContingencyContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CleanUpNetworkContextsTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpNetworkContextsTask.class);

    private NetworkContextRepository networkContextRepository;

    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;


    @Autowired
    public CleanUpNetworkContextsTask(NetworkContextRepository networkContextRepository, UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository
    ) {
        this.networkContextRepository = networkContextRepository;
        this.uiSnapshotContingencyContextRepository = uiSnapshotContingencyContextRepository;
    }

    @Getter
    @Value("${apogee.cleanupNetworkContexts.networkDateMaxAge:86400000}")
    private String networkDateMaxAge;

    @Getter
    @Value("${apogee.cleanupNetworkContexts.maxNumVersions:2}")
    private String maxNumVersions;


    @Transactional
    @Scheduled(initialDelayString = "${apogee.cleanupNetworkContexts.initialDelay}",
            fixedRateString = "${apogee.cleanupNetworkContexts.fixedRate}")
    public void run() {
        synchronized (UiSnapshotDaoImpl.SNAPSHOT_CREATION_LOCK) {
            try {
                LOGGER.info("Start removing old results");
                Instant currentDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                long criteriaDateLong = Long.parseLong(networkDateMaxAge);
                Instant dateFilter = currentDate.minus(criteriaDateLong, ChronoUnit.MILLIS);
                List<NetworkContext> networkContexts = networkContextRepository.findNetworkContextWithNetworkDateBeforeThan(dateFilter);
                networkContexts.forEach(networkContext -> {
                    uiSnapshotContingencyContextRepository.deleteUiSnapshotContingencyContextByNetworkContext(networkContext.getId());
                    uiSnapshotContingencyContextRepository.deleteUiSnapshotContextByNetworkContext(networkContext.getId());
                    networkContextRepository.delete(networkContext);
                    LOGGER.info("Deleting network context type={}, network_date={}, computation_date={}", networkContext.getCaseType().getName(), networkContext.getNetworkDate(), networkContext.getComputationDate());
                });

                cleanUpNetworkContextsWithmaxNumVersions(Integer.parseInt(maxNumVersions));

            } catch (NumberFormatException e) {
                LOGGER.error("Format networkDateMaxAge for deleting networkContext is wrong ", e);
            } catch (UnsupportedTemporalTypeException e) {
                LOGGER.error("The unit is not supported on creteriaDate for deleting networkContext ", e);
            } catch (DateTimeException | ArithmeticException e) {
                LOGGER.error("The subtraction cannot be made or numeric overflow occurs on creteriaDate for deleting networkContext ", e);

            } catch (RuntimeException e) {
                LOGGER.error("Problem on delete Network Context After Period ", e);
            }
            LOGGER.info("End removing old results");
        }
    }


    private void cleanUpNetworkContextsWithmaxNumVersions(int maxNumVersions) {
        List<NetworkContext> networkContexts = networkContextRepository.findAllOrderByCaseTypeAndOrderByNetworkDateAndOrderByComputationDateDesc();
        Map<PairTypeNetworkDate, Integer> map = new HashMap<>();
        networkContexts.forEach(networkContext -> {
            PairTypeNetworkDate pairTypeNetworkDate = new PairTypeNetworkDate(networkContext.getCaseType().getName(), networkContext.getNetworkDate());
            if (map.containsKey(pairTypeNetworkDate)) {
                int compt = map.get(pairTypeNetworkDate);
                if (compt >= maxNumVersions) {
                    uiSnapshotContingencyContextRepository.deleteUiSnapshotContingencyContextByNetworkContext(networkContext.getId());
                    uiSnapshotContingencyContextRepository.deleteUiSnapshotContextByNetworkContext(networkContext.getId());
                    networkContextRepository.delete(networkContext);
                    LOGGER.info("Deleting network context type={}, network_date={}, computation_date={}", networkContext.getCaseType().getName(), networkContext.getNetworkDate(), networkContext.getComputationDate());
                } else {
                    compt = compt + 1;
                    map.replace(pairTypeNetworkDate, compt);
                }

            } else {
                map.put(pairTypeNetworkDate, 1);
            }

        });
    }

    @EqualsAndHashCode
    private class PairTypeNetworkDate {
        String type;
        Instant networkDate;

        public PairTypeNetworkDate(String type, Instant networkDate) {
            this.type = type;
            this.networkDate = networkDate;
        }
    }
}
