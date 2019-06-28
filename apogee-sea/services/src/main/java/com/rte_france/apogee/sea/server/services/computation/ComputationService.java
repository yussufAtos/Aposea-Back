package com.rte_france.apogee.sea.server.services.computation;

import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.computation.logic.*;
import com.rte_france.apogee.sea.server.model.computation.variant.NetworkActionResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkLimitViolationRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.SnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UserRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeFilterDate;
import com.rte_france.apogee.sea.server.model.uisnapshot.Status;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingency;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingencyContext;
import com.rte_france.apogee.sea.server.model.user.User;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import com.rte_france.apogee.sea.server.services.exceptions.SnapshotNotFoundException;
import com.rte_france.apogee.sea.server.services.utility.TimerangeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComputationService implements IComputationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationService.class);

    private static final String UISNAPSHOT_NOT_EXIST = "This snapshot id does not exist in the database";

    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    private NetworkContingencyRepository networkContingencyRepository;

    private NetworkZoneRepository networkZoneRepository;

    private SnapshotRepository snapshotRepository;

    private UserRepository userRepository;

    private NetworkLimitViolationRepository networkLimitViolationRepository;

    @Autowired
    private UiSnapshotRepository uiSnapshotRepository;

    @Autowired
    private TimerangeFilter timerangeFilter;

    @Autowired
    public ComputationService(NetworkPostContingencyResultRepository networkPostContingencyResultRepository, UiSnapshotDaoImpl uiSnapshotDaoImpl,
                              NetworkLimitViolationRepository networkLimitViolationRepository, NetworkContingencyRepository networkContingencyRepository,
                              NetworkZoneRepository networkZoneRepository, SnapshotRepository snapshotRepository, UserRepository userRepository) {
        this.networkPostContingencyResultRepository = networkPostContingencyResultRepository;
        this.uiSnapshotDaoImpl = uiSnapshotDaoImpl;
        this.networkLimitViolationRepository = networkLimitViolationRepository;
        this.networkContingencyRepository = networkContingencyRepository;
        this.networkZoneRepository = networkZoneRepository;
        this.snapshotRepository = snapshotRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void insertDataSetInUiSnapshot() {
        uiSnapshotDaoImpl.handleUiSnapshotCreation();
    }

    @Override
    public SnapshotResult getMapNetworkContextByContingency(String username, int page, int size, String snapshotid, List<String> zones, String timerange, boolean exclude) throws ComputationServiceException {

        try {
            List<String> targetZones;
            //user check
            Optional<User> user = this.userRepository.findOneByUsername(username);
            Usertype actualUsertype = null;
            List<String> voltageLevelsToExclude;
            if (user.isPresent()) {
                targetZones = getTargztZones(user.get(), zones);
                actualUsertype = user.get().getActualUsertype();

            } else {
                targetZones = Collections.emptyList();
            }

            if (actualUsertype != null) {
                voltageLevelsToExclude = getVoltageLevelsToExclude(actualUsertype);
            } else {
                voltageLevelsToExclude = Collections.singletonList("");
            }
            //APOSEA-78 Passing empty list as parameter to JPA query throws error
            if (targetZones.isEmpty()) {
                targetZones.add("");
            }
            LOGGER.info("snapshotid={}, zones={}", snapshotid, targetZones);

            // get uiSnapshot by ID or the last if the uiSnapshot id is null
            UiSnapshot uiSnapshot = getUiSnapshot(snapshotid);
            Long uiSnapshotId = uiSnapshot.getId();

            TimerangeFilterDate timerangeFilterDate = null;
            if (timerange == null || timerange.isEmpty()) {
                timerangeFilterDate = timerangeFilter.getDateFilterByTimerange("Tout");
            } else {
                String timerangeString = URLDecoder.decode(timerange, StandardCharsets.UTF_8.toString());
                timerangeFilterDate = timerangeFilter.getDateFilterByTimerange(timerangeString);
            }

            //populate networkContexts filter by timerange
            List<NetworkContext> networkContexts = uiSnapshotDaoImpl.getNetworkRepository().fetchLastNetworkContextsWithPriorityByUiSnapshot(uiSnapshotId, timerangeFilterDate);

            //find all contingencies to exclure
            List<NetworkContingency> targetExcludeContingencies = getExcludeContingencyList(actualUsertype, targetZones, uiSnapshotId, timerangeFilterDate, exclude);

            //Get contingencies page filtring by zones, time range, snapshot id and contingencies in exclude zone
            Page<UiSnapshotContingency> networkUisnapshotContingenciesPage = getUiSnapshotContingencies(page, size, targetZones, targetExcludeContingencies, uiSnapshotId, timerangeFilterDate);

            //populate contingencies
            List<UiSnapshotContingency> uiSnapshotContingencies = networkUisnapshotContingenciesPage.getContent();
            List<ContingencyViolation> contingencies;
            if (!uiSnapshotContingencies.isEmpty()) {
                contingencies = populateContingencies(uiSnapshotContingencies, timerangeFilterDate);
            } else {
                contingencies = new ArrayList<>();
            }

            SnapshotResult snapshotContingencyContextByContingency = new SnapshotResult();
            snapshotContingencyContextByContingency.setNetworkContexts(networkContexts);

            if (page == 1) {
                //populate limitViolation-baseCase filtering by zone and exlude zone (the networkContexts are allready filter by timerange)
                List<LimitViolationByIdenfifier> mapLimitViolationBaseCases = getLimitViolationByIdenfifiers(networkContexts, targetZones, voltageLevelsToExclude, exclude);
                //set the number of contingency after the filter
                int contingenciesCount = uiSnapshotContingencies.size();

                snapshotContingencyContextByContingency.setPreContingencyLimitViolations(mapLimitViolationBaseCases);
                snapshotContingencyContextByContingency.setTotalRows(contingenciesCount + mapLimitViolationBaseCases.size());
            }
            snapshotContingencyContextByContingency.setUiSnapshot(uiSnapshot);
            snapshotContingencyContextByContingency.setContingencies(contingencies);
            snapshotContingencyContextByContingency.setTotalPages(networkUisnapshotContingenciesPage.getTotalPages());
            snapshotContingencyContextByContingency.setPageNumber(page);
            return snapshotContingencyContextByContingency;
        } catch (NumberFormatException e) {
            throw new ComputationServiceException("The values of pagination are wrong", e);
        } catch (UnsupportedEncodingException e) {
            throw new ComputationServiceException("The time range is wrong", e);
        }
    }

    /**
     * <p>Get uisnapshot contingency results from a snapshot.</p>
     * <p>a specified snapshot can be retrieved.</p>
     * <p>Results are grouped and paginated by contingencies.</p>
     *
     * @param page                       page number to retrieve, starts at page 1.
     * @param size                       page size.
     * @param targetZones                list of zones to filter.
     * @param targetExcludeContingencies list of contingencies to filter.
     * @param uiSnapshotId               snapshot ID.
     * @param timerangeFilterDate        time range to filter.
     * @return uisnapshot contingency results
     */
    private Page<UiSnapshotContingency> getUiSnapshotContingencies(int page, int size, List<String> targetZones, List<NetworkContingency> targetExcludeContingencies, Long uiSnapshotId, TimerangeFilterDate timerangeFilterDate) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("networkContingency.id").ascending());

        Page<UiSnapshotContingency> networkUisnapshotContingenciesPage;
        if (timerangeFilterDate.getStartDate() != null && timerangeFilterDate.getEndDate() != null) {
            networkUisnapshotContingenciesPage = snapshotRepository.getUiSnapshotContingencyRepository().findUiSnapshotContingenciesWithZonesAndTimerangeAndSnapshotIdAndPageableAndExcludeZones(targetZones, uiSnapshotId, pageable, timerangeFilterDate.getStartDate(), timerangeFilterDate.getEndDate(), targetExcludeContingencies);
        } else {
            networkUisnapshotContingenciesPage = snapshotRepository.getUiSnapshotContingencyRepository().findUiSnapshotContingenciesWithZonesAndSnapshotIdAndPageableAndExcludeZones(targetZones, uiSnapshotId, pageable, targetExcludeContingencies);
        }
        return networkUisnapshotContingenciesPage;
    }

    private List<ContingencyViolation> populateContingencies(List<UiSnapshotContingency> uiSnapshotContingencies, TimerangeFilterDate timerangeFilterDate) {
        List<ContingencyViolation> contingencies = new ArrayList<>();
        uiSnapshotContingencies.forEach(uiSnapshotContingency -> {
            Set<String> remedialsCandidates = new HashSet<>();
            Set<String> remedialsComputed = new HashSet<>();
            Set<String> remedialsEfficient = new HashSet<>();

            List<UiSnapshotContingencyContext> uiSnapshotContingencyContexts;
            if (timerangeFilterDate.getStartDate() != null) {
                uiSnapshotContingencyContexts = uiSnapshotContingency.getUiSnapshotContingencyContextList().stream().filter(uiSnapshotContingencyContext -> uiSnapshotContingencyContext.getUiSnapshotContext().getNetworkContext().getNetworkDate().isAfter(timerangeFilterDate.getStartDate())
                        && uiSnapshotContingencyContext.getUiSnapshotContext().getNetworkContext().getNetworkDate().isBefore(timerangeFilterDate.getEndDate())).collect(Collectors.toList());
            } else {
                uiSnapshotContingencyContexts = uiSnapshotContingency.getUiSnapshotContingencyContextList();
            }
            Collections.sort(uiSnapshotContingencyContexts);

            ContingencyViolation contingencyViolation = new ContingencyViolation();
            contingencyViolation.setContingency(uiSnapshotContingency.getNetworkContingency());
            ArraySnapshotContingencyContext arraySnapshotContingencyContext = new ArraySnapshotContingencyContext();
            uiSnapshotContingencyContexts.forEach(uiSnapshotContingencyContext -> {
                SnapshotContingencyContextIdentifier snapshotContingencyContextIdentifier = new SnapshotContingencyContextIdentifier();
                snapshotContingencyContextIdentifier.setNetworkContextId(uiSnapshotContingencyContext.getUiSnapshotContext().getNetworkContext().getId());
                snapshotContingencyContextIdentifier.setStatus(Status.valueOf(uiSnapshotContingencyContext.getStatus().toString()));
                arraySnapshotContingencyContext.add(snapshotContingencyContextIdentifier);
                // Added the remedials for each context
                remedialsCandidates.addAll(uiSnapshotContingencyContext.getRemedialsCandidates());
                remedialsComputed.addAll(uiSnapshotContingencyContext.getRemedialsComputed());
                remedialsEfficient.addAll(uiSnapshotContingencyContext.getRemedialsEfficient());
            });

            contingencyViolation.setCandidateRemedialsCount(remedialsCandidates.size());
            contingencyViolation.setComputedRemedialsCount(remedialsComputed.size());
            contingencyViolation.setEfficientRemedialsCount(remedialsEfficient.size());
            contingencyViolation.setViolationStatus(arraySnapshotContingencyContext);
            contingencies.add(contingencyViolation);
        });
        return contingencies;
    }


    private List<String> getTargztZones(User user, List<String> zones) {
        List<String> targetZones;

        List<String> userZones = new ArrayList<>();
        Usertype usertype = user.getActualUsertype();
        if (usertype != null) {
            Set<NetworkZone> networkZones = this.networkZoneRepository.findByUsertypes(usertype);
            networkZones.forEach(networkZone -> userZones.add(networkZone.getObjectid()));

            if (zones == null || zones.isEmpty()) {
                //get all user zones
                targetZones = userZones;
            } else {
                targetZones = getTargetZonesWhenNotEmpty(zones, userZones);
            }

        } else {
            targetZones = Collections.emptyList();
        }

        return targetZones;
    }

    private List<String> getTargetZonesWhenNotEmpty(List<String> zones, List<String> userZones) {
        List<String> targetZones;
        //zones check
        List<String> tmpZones = new ArrayList<>();

        zones.forEach(zone -> {
            try {
                if (userZones.contains(URLDecoder.decode(zone, "UTF-8"))) {
                    tmpZones.add(URLDecoder.decode(zone, "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                if (userZones.contains(zone)) {
                    tmpZones.add(zone);
                }
            }
        });

        targetZones = tmpZones;
        return targetZones;
    }

    private UiSnapshot getUiSnapshot(String snapshotid) {
        UiSnapshot uiSnapshot;
        if ((snapshotid != null) && !"".equals(snapshotid)) {
            Long uiSnapshotId = Long.parseLong(snapshotid);
            Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotDaoImpl.getUiSnapshotRepository().findById(uiSnapshotId);
            uiSnapshot = uiSnapshotOptional.orElseThrow(() -> new SnapshotNotFoundException(UISNAPSHOT_NOT_EXIST));

        } else {
            Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotRepository.findLatestUiSnapshot();
            uiSnapshot = uiSnapshotOptional.orElseThrow(() -> new SnapshotNotFoundException(UISNAPSHOT_NOT_EXIST));
        }
        return uiSnapshot;
    }

    /**
     * <p>Get contingencies to exclode.</p>
     *
     * @param usertype            the actual user type : if the actual user type is null or the exclude zone of the user type is empty then the list contains a contingency object with id =""
     * @param targetZones         list of zones to filter.
     * @param uiSnapshotId        snapshot ID.
     * @param timerangeFilterDate time range to filter.
     * @param exclude             the criterion for the filter :  if the value is false then the list contains a contingency object with id =""
     * @return list of contingency to exclude
     */
    private List<NetworkContingency> getExcludeContingencyList(Usertype usertype, List<String> targetZones, Long uiSnapshotId, TimerangeFilterDate timerangeFilterDate, boolean exclude) {
        //find all contingency to exlclude filtred by zones, uisnapshot and time range
        List<UiSnapshotContingency> allUisnapshotContingencies;
        if (timerangeFilterDate.getStartDate() != null && timerangeFilterDate.getEndDate() != null) {
            allUisnapshotContingencies = snapshotRepository.getUiSnapshotContingencyRepository().findUiSnapshotContingenciesWithZonesAndTimerangeAndSnapshotId(targetZones, uiSnapshotId, timerangeFilterDate.getStartDate(), timerangeFilterDate.getEndDate());
        } else {
            allUisnapshotContingencies = snapshotRepository.getUiSnapshotContingencyRepository().findUiSnapshotContingenciesWithZonesAndSnapshotId(targetZones, uiSnapshotId);
        }

        List<NetworkContingency> targetExcludeContingencies = new ArrayList<>();
        if (usertype != null && exclude && usertype.getExcludeZone() != null) {
            Set<NetworkVoltageLevel> voltageLevelsToExclude = usertype.getExcludeZone().getNetworkVoltageLevels();
            targetExcludeContingencies = allUisnapshotContingencies
                    .parallelStream()
                    .filter(uiSnapshotContingency ->
                            voltageLevelsToExclude.containsAll(uiSnapshotContingency.getNetworkContingency().getNetworkVoltageLevels()) ||
                                    voltageLevelsToExclude.containsAll(uiSnapshotContingency.fetchVoltageLevelsFromLimitViolations()))
                    .map(UiSnapshotContingency::getNetworkContingency)
                    .collect(Collectors.toList());
        }

        if (targetExcludeContingencies.isEmpty()) {
            targetExcludeContingencies.add(new NetworkContingency(""));
        }
        return targetExcludeContingencies;
    }

    private List<String> getVoltageLevelsToExclude(Usertype usertype) {
        List<String> voltageLevelsToExclude;
        if (usertype.getExcludeZone() != null) {
            voltageLevelsToExclude = new ArrayList<>();
            for (NetworkVoltageLevel networkVoltageLevel : usertype.getExcludeZone().getNetworkVoltageLevels()) {
                voltageLevelsToExclude.add(networkVoltageLevel.getObjectid());
            }
        } else {
            voltageLevelsToExclude = Collections.singletonList("");
        }
        return voltageLevelsToExclude;
    }

    @Override
    public LimitViolationByIdenfifierAndRemedials getLimitViolationsByContingency(Usertype actualUsertype, String contingencyId, String snapshotVersion, List<String> contextsId, boolean exclude) throws ComputationServiceException {

        Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotDaoImpl.getUiSnapshotRepository().findById(Long.parseLong(snapshotVersion));
        if (!uiSnapshotOptional.isPresent()) {
            throw new SnapshotNotFoundException("The snapshot id " + Long.parseLong(snapshotVersion) + " does not exist in the database");
        }

        try {
            LimitViolationByIdenfifierAndRemedials limitViolationByIdenfifierAndRemedials = new LimitViolationByIdenfifierAndRemedials();
            Map<String, RemedialResult> prioritizedRemedials = new TreeMap<>();
            List<List<Remedial>> remedials = new ArrayList<>();
            Optional<NetworkContingency> networkContingencyOptional = networkContingencyRepository.findById(contingencyId);
            if (!networkContingencyOptional.isPresent()) {
                throw new ComputationServiceException("The contingency id of request are wrong", new NoSuchElementException("the contingency id =" + contingencyId + "not exist in data base"));
            }

            List<Long> networkContextIds = new ArrayList<>();
            contextsId.forEach(networkcontextId -> networkContextIds.add(Long.parseLong(networkcontextId)));

            List<NetworkContext> networkContexts = snapshotRepository.getUiSnapshotContextRepository().findNetworkcontextByIds(networkContextIds);
            Map<NetworkContext, Set<NetworkLimitViolation>> mapNetworkContextLimitViolations = new HashMap<>();
            Set<NetworkLimitViolationIdentifier> allLimitViolations = new HashSet<>();

            for (NetworkContext networkContext : networkContexts) {
                Set<NetworkLimitViolation> networkLimitViolations = new HashSet<>();
                Optional<NetworkPostContingencyResult> networkPostContingencyResult = networkPostContingencyResultRepository.findByNetworkContextAndNetworkContingency(networkContext, networkContingencyOptional.get());
                if (networkPostContingencyResult.isPresent()) {
                    remedials.add(networkPostContingencyResult.get().getRemedials());
                    NetworkLimitViolationsResult networkLimitViolationsResult = networkPostContingencyResult.get().getNetworkLimitViolationsResult();

                    List<NetworkLimitViolation> limitViolations = null;
                    if (exclude && actualUsertype != null) {
                        limitViolations = networkLimitViolationsResult.getNetworkLimitViolationList()
                                .stream()
                                .filter(networkLimitViolation ->
                                        !actualUsertype.getExcludeZone().getNetworkVoltageLevels().containsAll(networkLimitViolation.getNetworkVoltageLevels())).collect(Collectors.toList());
                    } else {
                        limitViolations = networkLimitViolationsResult.getNetworkLimitViolationList();
                    }

                    limitViolations.forEach(networkLimitViolation -> {
                        NetworkLimitViolationIdentifier networkLimitViolationIdentifier = new NetworkLimitViolationIdentifier(networkLimitViolation.getSubjectId(), networkLimitViolation.getLimitType(),
                                networkLimitViolation.getAcceptableDuration(), networkLimitViolation.getSide());
                        allLimitViolations.add(networkLimitViolationIdentifier);
                        networkLimitViolations.add(networkLimitViolation);

                    });


                    networkLimitViolationsResult.getActionsResults().forEach(networkActionResult -> {
                        String remedial = networkActionResult.getRemedial().getShortDescription();

                        prioritizedRemedials.put(remedial, makeOrUpdateRemedialResult(prioritizedRemedials.get(remedial), remedial, networkActionResult, networkContext, networkContexts));
                    });
                }
                mapNetworkContextLimitViolations.put(networkContext, networkLimitViolations);
            }
            List<LimitViolationByIdenfifier> violations = populateLimitViolationsByIdentifiers(networkContexts, mapNetworkContextLimitViolations, allLimitViolations);
            limitViolationByIdenfifierAndRemedials.setCandidatesRemedials(remedials);
            limitViolationByIdenfifierAndRemedials.setRemedialsResults(prioritizedRemedials);
            limitViolationByIdenfifierAndRemedials.setViolations(violations);

            return limitViolationByIdenfifierAndRemedials;
        } catch (NumberFormatException e) {
            throw new ComputationServiceException("The values of request are wrong", e);
        }
    }

    private RemedialResult makeOrUpdateRemedialResult(RemedialResult remedialResult, String remedial, NetworkActionResult networkActionResult, NetworkContext networkContext, List<NetworkContext> networkContexts) {
        final RemedialResult result;

        if (remedialResult == null) {
            result = new RemedialResult(remedial);

            //init status by Context
            networkContexts.forEach(networkContextTmp -> result.getStatus().add(new RemedialResult.StatusByContext(networkContextTmp.getId())));

        } else {
            result = remedialResult;
        }

        //update violations
        networkActionResult.getVariantResult().getNetworkLimitViolationList().forEach(networkLimitViolation -> {
            NetworkLimitViolationIdentifier networkLimitViolationIdentifier = new NetworkLimitViolationIdentifier(networkLimitViolation.getSubjectId(), networkLimitViolation.getLimitType(),
                    networkLimitViolation.getAcceptableDuration(), networkLimitViolation.getSide());

            LimitViolationByIdenfifier limitViolationByIdenfifier = null;

            for (LimitViolationByIdenfifier limitViolationByIdenfifierTmp : result.getViolations()) {
                if (limitViolationByIdenfifierTmp.getIdentifier().equals(networkLimitViolationIdentifier)) {
                    limitViolationByIdenfifier = limitViolationByIdenfifierTmp;
                    break;
                }
            }

            if (limitViolationByIdenfifier == null) {
                limitViolationByIdenfifier = new LimitViolationByIdenfifier();
                limitViolationByIdenfifier.setIdentifier(networkLimitViolationIdentifier);

                List<NetworkContextLimitViolation> limitViolations = new ArrayList<>();

                networkContexts.forEach(networkContextTmp -> {
                    NetworkContextLimitViolation networkContextLimitViolation = new NetworkContextLimitViolation();
                    networkContextLimitViolation.setContextId(networkContextTmp.getId());
                    limitViolations.add(networkContextLimitViolation);
                });

                limitViolationByIdenfifier.setLimitViolations(limitViolations);

                result.getViolations().add(limitViolationByIdenfifier);

                result.getViolations().sort((l1, l2) -> l1.getIdentifier().getSubjectId().compareTo(l2.getIdentifier().getSubjectId()));
            }

            for (NetworkContextLimitViolation networkContextLimitViolation : limitViolationByIdenfifier.getLimitViolations()) {
                if (networkContextLimitViolation.getContextId() == networkContext.getId()) {
                    networkContextLimitViolation.setLimitViolation(networkLimitViolation);
                    break;
                }
            }
        });

        //update status
        updateStatus(result, networkContext, networkActionResult);

        return result;
    }

    private void updateStatus(RemedialResult result, NetworkContext networkContext, NetworkActionResult networkActionResult) {
        for (RemedialResult.StatusByContext statusByContext : result.getStatus()) {
            if (statusByContext.getContextId().longValue() == networkContext.getId().longValue()) {
                if (networkActionResult.getVariantResult().isComputationOk()) {
                    boolean hasViolations = !networkActionResult.isActionEfficient();
                    statusByContext.setStatus(hasViolations ? Status.V_R_AV : Status.NO_V);
                } else {
                    statusByContext.setStatus(Status.V_R_CMP_NOK);
                }

                break;
            }
        }
    }

    private List<LimitViolationByIdenfifier> populateLimitViolationsByIdentifiers(List<NetworkContext> networkContexts, Map<NetworkContext, Set<NetworkLimitViolation>> mapNetworkContextLimitViolations, Set<NetworkLimitViolationIdentifier> allLimitViolations) {
        List<LimitViolationByIdenfifier> limitViolationByIdenfifiers = new ArrayList<>();
        allLimitViolations.forEach(networkLimitViolationIdentifier -> {
            List<NetworkContextLimitViolation> limitViolationBaseCases = new ArrayList<>();
            networkContexts.forEach(networkContext -> {
                Set<NetworkLimitViolation> networkLimitViolations = mapNetworkContextLimitViolations.get(networkContext);
                NetworkContextLimitViolation networkContextLimitViolation = containsLimitViolation(networkLimitViolationIdentifier, networkLimitViolations);
                networkContextLimitViolation.setContextId(networkContext.getId());
                limitViolationBaseCases.add(networkContextLimitViolation);
            });
            LimitViolationByIdenfifier limitViolationByIdenfifier = new LimitViolationByIdenfifier();
            limitViolationByIdenfifier.setIdentifier(networkLimitViolationIdentifier);
            limitViolationByIdenfifier.setLimitViolations(limitViolationBaseCases);
            limitViolationByIdenfifiers.add(limitViolationByIdenfifier);
        });
        return limitViolationByIdenfifiers;
    }

    // Limit violation by subjectId, limit type, acceptableDuration, side
    private List<LimitViolationByIdenfifier> getLimitViolationByIdenfifiers(List<NetworkContext> networkContexts, List<String> zones, List<String> voltageLevelsToExclude, boolean exclude) {
        Map<NetworkContext, Set<NetworkLimitViolation>> mapNetworkContextLimitViolations = new HashMap<>();
        Set<NetworkLimitViolationIdentifier> allLimitViolations = new HashSet<>();
        networkContexts.forEach(networkContext -> {
            Set<NetworkLimitViolation> networkLimitViolations = new HashSet<>();
            List<NetworkLimitViolation> violation;
            if (exclude) {
                violation = networkLimitViolationRepository.findLimitViolationsResultByNetworkContextAndZonesAndZoneExclusion(networkContext, zones, voltageLevelsToExclude);
            } else {
                violation = networkLimitViolationRepository.findLimitViolationsResultByNetworkContextAndZones(networkContext, zones);
            }

            violation.forEach(networkLimitViolation -> {
                NetworkLimitViolationIdentifier networkLimitViolationIdentifier = new NetworkLimitViolationIdentifier(networkLimitViolation.getSubjectId(), networkLimitViolation.getLimitType(),
                        networkLimitViolation.getAcceptableDuration(), networkLimitViolation.getSide());
                allLimitViolations.add(networkLimitViolationIdentifier);
                networkLimitViolations.add(networkLimitViolation);
            });

            mapNetworkContextLimitViolations.put(networkContext, networkLimitViolations);
        });
        return populateLimitViolationsByIdentifiers(networkContexts, mapNetworkContextLimitViolations, allLimitViolations);
    }

    private NetworkContextLimitViolation containsLimitViolation(NetworkLimitViolationIdentifier networkLimitViolationIdentifier, Set<NetworkLimitViolation> networkLimitViolations) {
        NetworkContextLimitViolation networkContextLimitViolation = new NetworkContextLimitViolation();
        for (NetworkLimitViolation networkLimitViolation : networkLimitViolations) {
            NetworkLimitViolationIdentifier networkLimitViolationIdentifierTemp = new NetworkLimitViolationIdentifier(networkLimitViolation.getSubjectId(), networkLimitViolation.getLimitType(),
                    networkLimitViolation.getAcceptableDuration(), networkLimitViolation.getSide());
            if (networkLimitViolationIdentifier.equals(networkLimitViolationIdentifierTemp)) {
                networkContextLimitViolation.setLimitViolation(networkLimitViolation);
                return networkContextLimitViolation;
            }
        }
        return networkContextLimitViolation;
    }

    @Override
    public List<NetworkContext> fetchLastNetworkContextsWithPriority() {
        return uiSnapshotDaoImpl.getNetworkRepository().fetchLastNetworkContextsWithPriority();
    }


}
