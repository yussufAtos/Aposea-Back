package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolation;
import com.rte_france.apogee.sea.server.model.computation.NetworkPostContingencyResult;
import com.rte_france.apogee.sea.server.model.computation.variant.NetworkActionResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.PrioritizeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkVoltageLevelRepository;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedial;
import com.rte_france.apogee.sea.server.model.uisnapshot.*;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


@Service
public class UiSnapshotDaoImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(UiSnapshotDaoImpl.class);

    private static AtomicInteger queueSize = new AtomicInteger(0);

    //Attribute allows to synchronize the creation of snapshots and remove of old network contexts
    public static final Object SNAPSHOT_CREATION_LOCK = new Object();

    @Getter
    private UiSnapshotRepository uiSnapshotRepository;

    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    private NetworkVoltageLevelRepository networkVoltageLevelRepository;

    @Getter
    private NetworkRepository networkRepository;

    private UiSnapshotContextRepository uiSnapshotContextRepository;

    private PrioritizeRepository prioritizeRepository;

    private UiSnapshotDaoImpl self;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @PostConstruct
    public void postContruct() {
        self = applicationContext.getBean(UiSnapshotDaoImpl.class);
    }

    @Getter
    @Value("${apogee.uisnapshot.maxNumVersions:5}")
    private String maxNumVersions;

    @Autowired
    public UiSnapshotDaoImpl(UiSnapshotRepository uiSnapshotRepository, NetworkPostContingencyResultRepository networkPostContingencyResultRepository,
                             NetworkVoltageLevelRepository networkVoltageLevelRepository, NetworkRepository networkRepository,
                             UiSnapshotContextRepository uiSnapshotContextRepository, PrioritizeRepository prioritizeRepository) {
        this.uiSnapshotRepository = uiSnapshotRepository;
        this.networkPostContingencyResultRepository = networkPostContingencyResultRepository;
        this.networkVoltageLevelRepository = networkVoltageLevelRepository;
        this.networkRepository = networkRepository;
        this.uiSnapshotContextRepository = uiSnapshotContextRepository;
        this.prioritizeRepository = prioritizeRepository;
    }

    @Async("threadPoolTaskExecutor")
    public void asyncInsertDataSetInUiSnapshot() {
        synchronized (SNAPSHOT_CREATION_LOCK) {
            LOGGER.info("Asynchronous call in execution of the method asyncInsertDataSetInUiSnapshot");
            if (queueSize.compareAndSet(1, 0)) {
                self.insertDataSetInUiSnapshot();

                //publish event ui snapshot
                UiSnapshotEvent uiSnapshotEvent = new UiSnapshotEvent(this);
                applicationEventPublisher.publishEvent(uiSnapshotEvent);
            }
        }
        LOGGER.info("End of the asynchronous call of the method asyncInsertDataSetInUiSnapshot");
    }

    @Transactional
    public void insertDataSetInUiSnapshot() {
        this.deleteLatestVersion();
        List<NetworkContext> networkContexts = networkRepository.fetchLastNetworkContextsWithPriority();
        List<UiSnapshotContingency> uiSnapshotContingencyList = new ArrayList<>();
        Instant startedDate = Instant.now();
        UiSnapshot uiSnapshot = new UiSnapshot(startedDate);
        uiSnapshot.setUiSnapshotContingencyList(uiSnapshotContingencyList);

        Map<NetworkContext, Map<NetworkContingency, NetworkPostContingencyResult>> myMapContexts = new HashMap<>();
        Set<NetworkContingency> allContingencies = new HashSet<>();
        List<UiSnapshotContext> uiSnapshotContexts = new ArrayList<>();

        //Map voltage level with the zones
        Map<NetworkVoltageLevel, Set<NetworkZone>> voltageLevelZonesMap = new HashMap<>();
        List<NetworkVoltageLevel> networkVoltageLevelsAll = networkVoltageLevelRepository.findAll();
        networkVoltageLevelsAll.forEach(networkVoltageLevel -> voltageLevelZonesMap.put(networkVoltageLevel, networkVoltageLevel.getNetworkZones()));

        List<Prioritize> prioritizes = prioritizeRepository.findAll();
        Map<NetworkContext, Map<NetworkContingency, List<PrioritizeRemedial>>> mapPrioritizeRemedial = new HashMap<>();


        //Map context with the map of Contingencies and Map context with the map of Contingencies-PrioritizeRemedial list
        networkContexts.forEach(networkContext -> {
            UiSnapshotContext uiSnapshotContext = new UiSnapshotContext();
            uiSnapshotContext.setNetworkContext(networkContext);
            uiSnapshotContext.setUiSnapshot(uiSnapshot);
            uiSnapshotContexts.add(uiSnapshotContext);
            Map<NetworkContingency, NetworkPostContingencyResult> mapContingencies = new HashMap<>();
            Map<NetworkContingency, List<PrioritizeRemedial>> mapContingencyRemedial = new HashMap<>();
            List<NetworkPostContingencyResult> networkPostContingencyResults = networkPostContingencyResultRepository.findPostByNetworkContext(networkContext);
            //Map Contingency with the post contingency result
            networkPostContingencyResults.forEach(networkPostContingencyResult -> {

                //Map Contingency with the PrioritizeRemedial
                Optional<Prioritize> prioritizeOptional = prioritizes.stream().filter(prioritize ->
                        prioritize.getNetworkContingency().equals(networkPostContingencyResult.getNetworkContingency()) &&
                                (prioritize.getPrioritizeStartDate().equals(networkContext.getNetworkDate()) || prioritize.getPrioritizeStartDate().isBefore(networkContext.getNetworkDate())) &&
                                (prioritize.getPrioritizeEndDate() == null || prioritize.getPrioritizeEndDate().isAfter(networkContext.getNetworkDate()))
                ).findFirst();
                if (prioritizeOptional.isPresent()) {
                    mapContingencyRemedial.put(networkPostContingencyResult.getNetworkContingency(), prioritizeOptional.get().getPrioritizeRemedialList());
                } else {
                    mapContingencyRemedial.put(networkPostContingencyResult.getNetworkContingency(), new ArrayList<>());
                }

                mapContingencies.put(networkPostContingencyResult.getNetworkContingency(), networkPostContingencyResult);
                allContingencies.add(networkPostContingencyResult.getNetworkContingency());
            });

            mapPrioritizeRemedial.put(networkContext, mapContingencyRemedial);
            myMapContexts.put(networkContext, mapContingencies);
        });

        allContingencies.forEach(networkContingency -> {
            Set allZones = new HashSet<>();
            Set<NetworkVoltageLevel> networkVoltageLevels = networkContingency.getNetworkVoltageLevels();
            setZonesInSnapshot(allZones, networkVoltageLevels, voltageLevelZonesMap);

            UiSnapshotContingency uiSnapshotContingency = new UiSnapshotContingency(networkContingency);
            uiSnapshotContingency.setUiSnapshot(uiSnapshot);
            uiSnapshot.getUiSnapshotContingencyList().add(uiSnapshotContingency);

            //Create uiSnapshotContingencyContexts for uiSnapshotContext
            uiSnapshotContexts.forEach(uiSnapshotContext -> {
                Set<String> remedialsCandidates = new HashSet<>();
                Map<NetworkContingency, NetworkPostContingencyResult> mapContingencies = myMapContexts.get(uiSnapshotContext.getNetworkContext());
                // Populate remedialsCandidates
                setRemedialsCandidates(networkContingency, remedialsCandidates, mapContingencies);

                UiSnapshotContingencyContext uiSnapshotContingencyContext = createUiSnapshotContingencyContext(mapContingencies, voltageLevelZonesMap,
                        allZones, mapPrioritizeRemedial.get(uiSnapshotContext.getNetworkContext()), networkContingency);

                //Update status of uiSnapshotContingencyContext on violations
                List<NetworkActionResult> actionResultsBycontext = getActionResultByContext(networkContingency, mapContingencies);
                if (uiSnapshotContingencyContext.getStatus().equals(Status.V_R_AV) && (remedialsCandidates.size() - actionResultsBycontext.size() <= 0)) {
                    uiSnapshotContingencyContext.setStatus(Status.V_NO_R_AV);
                }
                uiSnapshotContingency.getUiSnapshotContingencyContextList().add(uiSnapshotContingencyContext);
                uiSnapshotContingencyContext.setUiSnapshotContext(uiSnapshotContext);
                uiSnapshotContingencyContext.setUiSnapshotContingency(uiSnapshotContingency);
                uiSnapshotContingencyContext.setRemedialsCandidates(remedialsCandidates);
            });
            uiSnapshotContingency.getNetworkZones().addAll(allZones);

        });
        LOGGER.info("UiSnapshotDaoImpl : Creating uiSnapshots");
        Instant createdDate = Instant.now();
        uiSnapshot.setCreatedDate(createdDate);
        UiSnapshot uiSnapshotDataBase = uiSnapshotRepository.saveAndFlush(uiSnapshot);
        if (allContingencies.isEmpty()) {
            uiSnapshotContexts.forEach(uiSnapshotContext -> uiSnapshotContextRepository.save(uiSnapshotContext));
        }
        LOGGER.info("UiSnapshotDaoImpl : Created uiSnapshot, date created ={}", uiSnapshotDataBase.getCreatedDate());
    }

    private List<NetworkActionResult> getActionResultByContext(NetworkContingency networkContingency, Map<NetworkContingency, NetworkPostContingencyResult> mapContingencies) {
        List<NetworkActionResult> actionResultsBycontxt = new ArrayList<>();
        if (mapContingencies.containsKey(networkContingency) && !mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().getActionsResults().isEmpty()) {
            actionResultsBycontxt = mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().getActionsResults();

        }
        return actionResultsBycontxt;
    }

    private void setRemedialsCandidates(NetworkContingency networkContingency, Set<String> remedialsCandidates, Map<NetworkContingency, NetworkPostContingencyResult> mapContingencies) {
        if (mapContingencies.containsKey(networkContingency) && !mapContingencies.get(networkContingency).getRemedials().isEmpty()) {
            mapContingencies.get(networkContingency).getRemedials().forEach(remedial -> remedialsCandidates.add(remedial.getIdRemedialRepository()));
        }
    }

    private UiSnapshotContingencyContext createUiSnapshotContingencyContext(Map<NetworkContingency, NetworkPostContingencyResult> mapContingencies, Map<NetworkVoltageLevel,
            Set<NetworkZone>> voltageLevelZonesMap, Set allZones, Map<NetworkContingency, List<PrioritizeRemedial>> mapContingencyRemedial, NetworkContingency networkContingency) {

        UiSnapshotContingencyContext uiSnapshotContingencyContext = new UiSnapshotContingencyContext();
        Set<String> remedialsComputed = new HashSet<>();
        Set<String> remedialsEfficient = new HashSet<>();
        List<PrioritizeRemedial> prioritizeRemedials = mapContingencyRemedial.get(networkContingency);

        if (mapContingencies.containsKey(networkContingency)) {
            List<NetworkActionResult> networkActionResults = mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().getActionsResults();
            boolean noViolations = mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().getNetworkLimitViolationList().isEmpty();
            boolean computationOk = mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().isComputationOk();
            Status status;

            if (!networkActionResults.isEmpty()) {
                status = getStatusActionResult(networkActionResults, prioritizeRemedials, remedialsComputed, remedialsEfficient);
                uiSnapshotContingencyContext.setStatus(status);
            } else {
                if (!computationOk) {
                    uiSnapshotContingencyContext.setStatus(Status.C_CMP_NOK);
                } else if (noViolations) {
                    uiSnapshotContingencyContext.setStatus(Status.NO_V);
                } else {
                    List<NetworkLimitViolation> limitViolations = mapContingencies.get(networkContingency).getNetworkLimitViolationsResult().getNetworkLimitViolationList();
                    limitViolations.forEach(networkLimitViolation -> {
                        Set<NetworkVoltageLevel> networkVoltageLevelsViolations = networkLimitViolation.getNetworkVoltageLevels();
                        setZonesInSnapshot(allZones, networkVoltageLevelsViolations, voltageLevelZonesMap);
                    });
                    uiSnapshotContingencyContext.setStatus(Status.V_R_AV);
                }
            }

        } else {
            uiSnapshotContingencyContext.setStatus(Status.NO_V);
        }

        uiSnapshotContingencyContext.setRemedialsComputed(remedialsComputed);
        uiSnapshotContingencyContext.setRemedialsEfficient(remedialsEfficient);
        return uiSnapshotContingencyContext;
    }

    private void setZonesInSnapshot(Set allZones, Set<NetworkVoltageLevel> networkVoltageLevels, Map<NetworkVoltageLevel, Set<NetworkZone>> voltageLevelZonesMap) {
        networkVoltageLevels.forEach(networkVoltageLevel -> allZones.addAll(voltageLevelZonesMap.get(networkVoltageLevel)));
    }

    private void deleteLatestVersion() {
        long versions = uiSnapshotRepository.count();
        long versionsMax = Long.parseLong(maxNumVersions);
        while (versions >= versionsMax) {
            long id = uiSnapshotRepository.findOldesestId();
            LOGGER.info("Uisnapshot deleting all uisnapshot of version uisnapshotId={}", id);
            uiSnapshotRepository.deleteById(id);
            LOGGER.info("Uisnapshot deleted all uisnapshot of version uisnapshotId={}", id);
            versions = versions - 1;
        }
    }

    private Status getStatusActionResult(List<NetworkActionResult> actionResults, List<PrioritizeRemedial> prioritizeRemedials, Set<String> remedialsComputed, Set<String> remedialsEfficient) {
        Status status;
        boolean remedial1IsEfficient = false;
        int paradeEfficient = 0;
        int divergence = 0;
        for (NetworkActionResult networkActionResult : actionResults) {
            remedialsComputed.add(networkActionResult.getRemedial().getIdRemedialRepository());
            if (networkActionResult.isActionEfficient() && !prioritizeRemedials.isEmpty() && prioritizeRemedials.get(0).getRemedial().equals(networkActionResult.getRemedial()) && networkActionResult.getVariantResult().isComputationOk()) {
                remedial1IsEfficient = true;
            }
            if (networkActionResult.getVariantResult().isComputationOk()) {
                if (networkActionResult.isActionEfficient()) {
                    remedialsEfficient.add(networkActionResult.getRemedial().getIdRemedialRepository());
                    paradeEfficient = paradeEfficient + 1;
                }
            } else {
                divergence = divergence + 1;
            }
        }

        if (divergence == actionResults.size()) {
            status = Status.V_R_CMP_NOK;
        } else if (paradeEfficient == 0) {
            status = Status.V_R_AV;
        } else if (prioritizeRemedials.isEmpty()) {
            status = Status.V_RX_EFF;
        } else if (!remedial1IsEfficient) {
            status = Status.V_RX_EFF;
        } else {
            status = Status.V_R1_EFF;
        }

        return status;
    }

    public void handleUiSnapshotCreation() {
        if (queueSize.compareAndSet(0, 1)) {
            LOGGER.info("Asynchronous call demanded of the method asyncInsertDataSetInUiSnapshot");
            asyncInsertDataSetInUiSnapshot();
        } else {
            LOGGER.info("Asynchronous call already demanded of the method asyncInsertDataSetInUiSnapshot");
        }
    }
}

