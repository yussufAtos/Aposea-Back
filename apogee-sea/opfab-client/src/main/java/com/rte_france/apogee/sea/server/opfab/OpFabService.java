package com.rte_france.apogee.sea.server.opfab;

import com.rte_france.apogee.sea.server.model.card.*;
import com.rte_france.apogee.sea.server.model.computation.CaseType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkLimitViolation;
import com.rte_france.apogee.sea.server.model.computation.logic.NetworkLimitViolationIdentifier;
import com.rte_france.apogee.sea.server.model.computation.variant.NetworkActionResult;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseTypeRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkLimitViolationRepository;
import com.rte_france.apogee.sea.server.model.dao.timerange.TimerangeTypeRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContingencyContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.model.timerange.TimerangeType;
import com.rte_france.apogee.sea.server.model.uisnapshot.Status;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshot;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingencyContext;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotEvent;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import com.rte_france.apogee.sea.server.opfab.client.web.api.CardsApiClient;
import com.rte_france.apogee.sea.server.opfab.client.web.model.*;
import com.rte_france.apogee.sea.server.opfab.util.OpFabProperties;
import com.rte_france.apogee.sea.server.services.utility.TimerangeFilter;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class OpFabService implements IOpFabService, ApplicationListener<UiSnapshotEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpFabService.class);

    @Autowired
    OpFabProperties opFabProperties;

    Map<Pair<NetworkContext, NetworkContingency>, List<NetworkLimitViolation>> mapLimitViolationsByContextAndContingency = new HashMap<>();

    Map<Pair<NetworkContext, NetworkContingency>, List<NetworkActionResult>> mapNetworkActionResultsByContextAndContingency = new HashMap<>();

    @Autowired
    private UiSnapshotRepository uiSnapshotRepository;

    @Autowired
    private UiSnapshotContextRepository uiSnapshotContextRepository;

    @Autowired
    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;

    @Autowired
    CaseTypeRepository caseTypeRepository;

    @Autowired
    TimerangeTypeRepository timerangeTypeRepository;

    @Autowired
    UsertypeRepository usertypeRepository;

    @Autowired
    NetworkLimitViolationRepository networkLimitViolationRepository;

    @Autowired
    private TimerangeFilter timerangeFilter;

    List<SeverityEnum> severityEnums = Arrays.asList(SeverityEnum.ALARM, SeverityEnum.QUESTION, SeverityEnum.NOTIFICATION);

    //Attribute allows to synchronize the push cards invoke
    public static final Object PUSH_CARDS_LOCK = new Object();

    private CardsApiClient cardsApiClient;

    public OpFabService(CardsApiClient cardsApiClient) {
        this.cardsApiClient = cardsApiClient;
    }

    @Override
    public void onApplicationEvent(UiSnapshotEvent uiSnapshotEvent) {
        synchronized (PUSH_CARDS_LOCK) {
            LOGGER.info("call of the method pushCards");

            try {
                if (opFabProperties.getServiceEnable()) {
                    long startTime = System.currentTimeMillis();

                    pushCards();

                    long timeTaken = System.currentTimeMillis() - startTime;
                    LOGGER.info("Pushing cards to OpFab took {} ms", timeTaken);
                } else {
                    LOGGER.info("OpFab service is disabled");
                }

            } catch (OpFabServiceException e) {
                LOGGER.error(e.getMessage());
            }
        }
        LOGGER.info("End of the call of the method pushCards");
    }

    @Override
    public void pushCards() throws OpFabServiceException {

        try {
            Set<Card> cards = new HashSet<>();

            //fetch contexts from latest uisnapshot
            Optional<UiSnapshot> uiSnapshotOptional = uiSnapshotRepository.findLatestUiSnapshot();
            UiSnapshot latestUiSnapshot = uiSnapshotOptional.orElseThrow(() -> new OpFabServiceException("The snapshot does not exist in the database"));
            List<NetworkContext> networkContexts = uiSnapshotContextRepository.findLatestContextByUiSnapshotId(latestUiSnapshot.getId());

            getGenericTypes().forEach(genericType -> {
                usertypeRepository.findAll(new Sort(Sort.Direction.ASC, "name")).stream().filter(Usertype::isOpfabEnabled).forEach(userType -> {

                    severityEnums.forEach(severityEnum -> {

                        long startTime = System.currentTimeMillis();

                        //filter network context list by case type
                        List<NetworkContext> networkContextsByGenericType = filterNetworkContexts(genericType, networkContexts);

                        Object cardData = prepareCardDataObject(genericType, userType, severityEnum, networkContextsByGenericType, latestUiSnapshot);

                        if (cardData != null) {
                            Card card = prepareCard(genericType, userType, severityEnum, networkContextsByGenericType, cardData);

                            long timeTaken = System.currentTimeMillis() - startTime;

                            LOGGER.info("Prepare card for {}-{}-{}-{} ({} ms)", genericType.processId, genericType.getName().toUpperCase(), userType.getName().toUpperCase(), severityEnum.name().toUpperCase(), timeTaken);
                            cards.add(card);
                        } else {
                            Card card = prepareEmptyCard(genericType, userType, severityEnum);

                            long timeTaken = System.currentTimeMillis() - startTime;

                            LOGGER.info("Prepare empty card for {}-{}-{}-{} ({} ms)", genericType.processId, genericType.getName().toUpperCase(), userType.getName().toUpperCase(), severityEnum.name().toUpperCase(), timeTaken);
                            cards.add(card);
                        }
                    });

                    //push cards
                    if (!cards.isEmpty()) {
                        LOGGER.info("push cards (userType={}, {}, size={})", userType.getName(), genericType, cards.size());
                        cardsApiClient.publishCardsAsync(cards.stream().collect(Collectors.toList()));
                    } else {
                        LOGGER.info("no cards to push (userType={}, {})", userType.getName(), genericType);
                    }

                    cards.clear();
                });
            });

        } finally {
            //clear structures
            mapLimitViolationsByContextAndContingency.clear();
            mapNetworkActionResultsByContextAndContingency.clear();
        }

    }

    private List<NetworkContext> filterNetworkContexts(GenericType genericType, List<NetworkContext> networkContexts) {
        if (genericType.isCaseType) {
            return networkContexts.stream().filter(networkContext -> networkContext.getCaseType().getName().equals(genericType.getName())).collect(Collectors.toList());
        } else {
            return timerangeFilter.filterNetworkContextsByTimerangeType(genericType.getName(), networkContexts);
        }
    }

    private List<GenericType> getGenericTypes() {
        List<GenericType> genericTypes = new ArrayList<>();

        caseTypeRepository.findByEnabledTrueAndOpfabEnabledTrueOrderByCaseCategory_DisplayPriority().forEach(caseType -> genericTypes.add(new GenericType(caseType)));

        timerangeTypeRepository.findByOpfabEnabledTrue().forEach(timerangeType -> genericTypes.add(new GenericType(timerangeType)));

        return genericTypes;
    }

    private Object prepareCardDataObject(GenericType genericType, Usertype userType, SeverityEnum severityEnum, List<NetworkContext> networkContexts, UiSnapshot uiSnapshot) {

        Object cardData;

        //PF case type return list
        if (genericType.isCaseType && genericType.getName().equalsIgnoreCase("PF")) {
            //sort PF network context list
            List<NetworkContext> networkContextsSorted = networkContexts.stream().sorted(Comparator.comparing(NetworkContext::getComputationDate).reversed()).collect(Collectors.toList());

            Optional<List<CardData>> optionalCardDataList = preparePFCardData(severityEnum, userType, networkContextsSorted, uiSnapshot);
            cardData = optionalCardDataList.isPresent() ? optionalCardDataList.get() : null;
        } else {
            Optional<CardData> optionalCardData = prepareCardData(severityEnum, userType, networkContexts, uiSnapshot);
            cardData = optionalCardData.isPresent() ? optionalCardData.get() : null;
        }

        return cardData;
    }

    private Card prepareEmptyCard(GenericType genericType, Usertype userType, SeverityEnum severityEnum) {
        Card card = new Card();
        card.setPublisher(opFabProperties.getPublisherName());
        card.setPublisherVersion(opFabProperties.getPublisherVersion());
        card.processId(getProcessId(genericType, userType.getName(), severityEnum));

        Instant now = Instant.now();
        card.setStartDate(now.toEpochMilli());
        card.setEndDate(now.toEpochMilli());

        //just to be consistent with the supervision colors
        if (severityEnum.equals(SeverityEnum.QUESTION)) {
            card.setSeverity(SeverityEnum.ACTION);
        } else if (severityEnum.equals(SeverityEnum.NOTIFICATION)) {
            card.setSeverity(SeverityEnum.QUESTION);
        } else {
            card.setSeverity(severityEnum);
        }

        card.setTags(getTags(genericType));
        card.setTitle(getTitle(genericType, userType.getName()));

        card.setSummary(getSummaryEmptyCard());

        card.setData("");
        card.setDetails(getDetailsEmptyCard());

        card.setRecipient(getRecipient(userType));
        return card;
    }

    private Card prepareCard(GenericType genericType, Usertype userType, SeverityEnum severityEnum, List<NetworkContext> networkContextsByGenericType, Object cardData) {
        Card card = new Card();
        card.setPublisher(opFabProperties.getPublisherName());
        card.setPublisherVersion(opFabProperties.getPublisherVersion());
        card.processId(getProcessId(genericType, userType.getName(), severityEnum));

        //card start/end date
        card.setStartDate(networkContextsByGenericType.get(0).getNetworkDate().plus(genericType.getStartDateIncrement(), ChronoUnit.MINUTES).toEpochMilli());
        card.setEndDate(networkContextsByGenericType.get(networkContextsByGenericType.size() - 1).getNetworkDate().plus(genericType.getEndDateIncrement(), ChronoUnit.MINUTES).toEpochMilli());

        //just to be consistent with the supervision colors
        if (severityEnum.equals(SeverityEnum.QUESTION)) {
            card.setSeverity(SeverityEnum.ACTION);
        } else if (severityEnum.equals(SeverityEnum.NOTIFICATION)) {
            card.setSeverity(SeverityEnum.QUESTION);
        } else {
            card.setSeverity(severityEnum);
        }

        card.setTags(getTags(genericType));
        card.setTitle(getTitle(genericType, userType.getName()));

        Integer networkContingenciesSize = cardData instanceof CardData ?
                ((CardData) cardData).getNetworkContingencies().size() : ((List<CardData>) cardData).get(0).getNetworkContingencies().size();
        card.setSummary(getSummary(networkContingenciesSize));

        card.setData(cardData);
        card.setDetails(getDetails(genericType, networkContextsByGenericType));

        card.setRecipient(getRecipient(userType));
        return card;
    }

    private Recipient getRecipient(Usertype usertype) {
        Recipient recipientGroup = new Recipient();
        recipientGroup.setType(RecipientEnum.GROUP);
        recipientGroup.setIdentity(usertype.getName());

        return recipientGroup;
    }

    private String getProcessId(GenericType genericType, String userType, SeverityEnum severityEnum) {
        return genericType.getProcessId() + '-' + genericType.getName().toUpperCase() + "-" + userType.toUpperCase() + "-" + severityEnum.name().toUpperCase();
    }

    public List<String> getTags(GenericType genericType) {
        List<String> tags = new ArrayList<>(opFabProperties.getTags());

        if (genericType.isTimerangeType) {
            //time range tag
            tags.add(genericType.getTimerangeType().getCardTag());
        } else {
            //case type tag
            tags.add(genericType.getCaseType().getCardTag());
        }

        return tags;
    }


    public I18n getTitle(GenericType genericType, String userType) {
        I18n title = new I18n();
        title.setKey(opFabProperties.getTitleKey());

        if (!opFabProperties.getTitleParameters().isEmpty()) {
            String titleParam = genericType.getName().toUpperCase() + " - " + userType.toUpperCase();
            title.putParametersItem(opFabProperties.getTitleParameters().get(0), titleParam);
        }

        return title;
    }

    public I18n getSummary(Integer networkContingenciesSize) {
        I18n summary = new I18n();

        if (networkContingenciesSize == 0 || networkContingenciesSize == 1) {
            String key = opFabProperties.getSummaryStatic() + "." + networkContingenciesSize;
            summary.setKey(key);
        } else {
            summary.setKey(opFabProperties.getSummaryKey());

            if (!opFabProperties.getSummaryParameters().isEmpty()) {
                summary.putParametersItem(opFabProperties.getSummaryParameters().get(0), networkContingenciesSize.toString());
            }
        }

        return summary;
    }

    private I18n getSummaryEmptyCard() {
        I18n summary = new I18n();

        String key = opFabProperties.getSummaryStatic() + ".0";
        summary.setKey(key);

        return summary;
    }

    public List<Detail> getDetails(GenericType genericType, List<NetworkContext> networkContexts) {
        List<Detail> details = new ArrayList<>();

        if (genericType.isCaseType && genericType.getName().equalsIgnoreCase("PF")) {
            for (int i = 0; i < opFabProperties.getMaxTabNumber(); i++) {
                if (i < networkContexts.size()) {
                    NetworkContext networkContext = networkContexts.get(i);

                    I18n titlePf = new I18n();
                    titlePf.key(opFabProperties.getDetailsPfKey() + "." + (i + 1));

                    if (!opFabProperties.getDetailsPfParameters().isEmpty()) {
                        titlePf.putParametersItem(opFabProperties.getDetailsPfParameters().get(0), Long.toString(networkContext.getNetworkDate().getEpochSecond()));
                    }

                    Detail detail = new Detail();
                    detail.setTitle(titlePf);
                    detail.setTemplateName(opFabProperties.getTemplateTabName() + (i + 1));
                    detail.setStyles(Arrays.asList(opFabProperties.getStyleName()));

                    details.add(detail);
                }
            }
        } else {
            I18n titleGeneric = new I18n();
            titleGeneric.key(opFabProperties.getDetailsGenericKey());

            Detail detail = new Detail();
            detail.setTitle(titleGeneric);
            detail.setTemplateName(opFabProperties.getTemplateName());
            detail.setStyles(Arrays.asList(opFabProperties.getStyleName()));

            details.add(detail);
        }

        return details;
    }

    private List<Detail> getDetailsEmptyCard() {
        List<Detail> details = new ArrayList<>();

        I18n titleGeneric = new I18n();
        titleGeneric.key(opFabProperties.getDetailsGenericKey());

        Detail detail = new Detail();
        detail.setTitle(titleGeneric);
        detail.setTemplateName(opFabProperties.getTemplateName());
        detail.setStyles(Arrays.asList(opFabProperties.getStyleName()));

        details.add(detail);

        return details;
    }

    private Optional<List<CardData>> preparePFCardData(SeverityEnum severityEnum, Usertype userType, List<NetworkContext> networkContexts, UiSnapshot latestUiSnapshot) {
        List<CardData> cardDataList = new ArrayList<>();

        IntStream.range(0, networkContexts.size()).forEach(i -> {
            Optional<CardData> cardDataOptional = prepareCardData(severityEnum, userType, Arrays.asList(networkContexts.get(i)), latestUiSnapshot);

            if (cardDataOptional.isPresent()) {
                CardData cardData = cardDataOptional.get();
                cardData.setPfPosition(i + 1);

                cardDataList.add(cardData);
            }
        });

        return cardDataList.isEmpty() ? Optional.empty() : Optional.of(cardDataList);
    }

    private Optional<CardData> prepareCardData(SeverityEnum severityEnum, Usertype userType, List<NetworkContext> networkContexts, UiSnapshot latestUiSnapshot) {

        if (networkContexts.isEmpty()) {
            return Optional.empty();
        }

        List<Status> statuses = new ArrayList<>();
        switch (severityEnum) {
            case ALARM:
                statuses.add(Status.C_CMP_NOK); //purple
                statuses.add(Status.V_R_CMP_NOK); //purple
                statuses.add(Status.V_R_AV); //red light
                statuses.add(Status.V_NO_R_AV); //red dark
                break;
            case QUESTION:
                statuses.add(Status.V_RX_EFF); //orange
                break;
            case NOTIFICATION:
                statuses.add(Status.V_R1_EFF); //green
                break;
            default:
                break;
        }

        //I- Pre contingencies
        List<CardNetworkLimitViolation> networkLimitViolationsN = prepareCardNetworkLimitViolationsN(severityEnum, networkContexts, userType);

        //II- Post contingencies
        List<CardNetworkContingency> cardNetworkContingencies = prepareCardNetworkContingencies(userType, networkContexts, latestUiSnapshot, statuses);


        if ((networkLimitViolationsN == null || networkLimitViolationsN.isEmpty()) && cardNetworkContingencies.isEmpty()) {
            return Optional.empty();
        }

        CardData cardData = new CardData(networkLimitViolationsN, cardNetworkContingencies, null);

        return Optional.of(cardData);
    }

    private List<CardNetworkLimitViolation> prepareCardNetworkLimitViolationsN(SeverityEnum severityEnum, List<NetworkContext> networkContexts, Usertype userType) {
        List<CardNetworkLimitViolation> networkLimitViolationsN = null;

        Set<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiers = new HashSet<>();
        Map<Pair<NetworkContext, NetworkLimitViolationIdentifier>, NetworkLimitViolation> mapLimitViolationByContextAndLimitViolationIdentifier = new HashMap<>();

        if (severityEnum.equals(SeverityEnum.ALARM)) {
            List<String> objectidsZones = userType.getNetworkZones().stream().map(NetworkZone::getObjectid).collect(Collectors.toList());

            networkContexts.forEach(networkContext -> {
                List<NetworkLimitViolation> networkLimitViolations = networkLimitViolationRepository.findLimitViolationsResultByNetworkContextAndZones(networkContext, objectidsZones);

                prepareStructure(networkContext, networkLimitViolations, networkLimitViolationIdentifiers, mapLimitViolationByContextAndLimitViolationIdentifier);
            });

            networkLimitViolationsN = makeCardNetworkLimitViolations(networkLimitViolationIdentifiers, networkContexts, mapLimitViolationByContextAndLimitViolationIdentifier);
        }
        return networkLimitViolationsN;
    }

    private List<CardNetworkContingency> prepareCardNetworkContingencies(Usertype userType, List<NetworkContext> networkContexts, UiSnapshot latestUiSnapshot, List<Status> statuses) {
        List<CardNetworkContingency> cardNetworkContingencies = new ArrayList<>();

        Set<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiers = new HashSet<>();
        Map<Pair<NetworkContext, NetworkLimitViolationIdentifier>, NetworkLimitViolation> mapLimitViolationByContextAndLimitViolationIdentifier = new HashMap<>();

        Set<UiSnapshotContingencyContext> uiSnapshotContingencyContexts = new HashSet<>();

        userType.getNetworkZones().forEach(networkZone -> uiSnapshotContingencyContexts.addAll(
                uiSnapshotContingencyContextRepository.findAllByNetworkContextsAndNetworkZonesAnAndStatusList(networkContexts, networkZone, statuses, latestUiSnapshot.getId())));

        Set<NetworkContingency> networkContingencies = new HashSet<>();
        uiSnapshotContingencyContexts.forEach(uiSnapshotContingencyContext ->
                networkContingencies.add(uiSnapshotContingencyContext.getUiSnapshotContingency().getNetworkContingency()));

        networkContingencies.stream().sorted(Comparator.comparing(NetworkContingency::getId)).forEach(networkContingency -> {

            //II.1 Limit violations by contingency
            networkLimitViolationIdentifiers.clear();
            mapLimitViolationByContextAndLimitViolationIdentifier.clear();

            networkContexts.forEach(networkContext -> {

                List<NetworkLimitViolation> networkLimitViolations;

                if (mapLimitViolationsByContextAndContingency.containsKey(Pair.of(networkContext, networkContingency))) {
                    networkLimitViolations = mapLimitViolationsByContextAndContingency.get(Pair.of(networkContext, networkContingency));

                } else {
                    networkLimitViolations = networkLimitViolationRepository.findAllByNetworkContextByNetworkContingencyAndZoneExclusion(networkContext, networkContingency, Collections.singletonList(""));
                    mapLimitViolationsByContextAndContingency.put(Pair.of(networkContext, networkContingency), networkLimitViolations);
                }

                prepareStructure(networkContext, networkLimitViolations, networkLimitViolationIdentifiers, mapLimitViolationByContextAndLimitViolationIdentifier);
            });

            List<CardNetworkLimitViolation> cardNetworkLimitViolations = makeCardNetworkLimitViolations(networkLimitViolationIdentifiers, networkContexts, mapLimitViolationByContextAndLimitViolationIdentifier);


            //II.2 Remedials by contingency
            List<CardNetworkRemedial> cardNetworkRemedials = new ArrayList<>();

            Set<Remedial> remedials = new HashSet<>();

            networkContexts.forEach(networkContext -> {

                if (!mapNetworkActionResultsByContextAndContingency.containsKey(Pair.of(networkContext, networkContingency))) {
                    List<NetworkActionResult> networkActionResultsTmp = new ArrayList<>();

                    mapLimitViolationsByContextAndContingency.get(Pair.of(networkContext, networkContingency)).stream().findFirst().ifPresent(networkLimitViolation ->
                            networkActionResultsTmp.addAll(networkLimitViolation.getNetworkLimitViolationsResult().getActionsResults()));

                    mapNetworkActionResultsByContextAndContingency.put(Pair.of(networkContext, networkContingency), networkActionResultsTmp);
                }

                remedials.addAll(mapNetworkActionResultsByContextAndContingency.get(Pair.of(networkContext, networkContingency))
                        .stream().map(NetworkActionResult::getRemedial).collect(Collectors.toList()));
            });

            remedials.stream().sorted(Comparator.comparing(Remedial::getShortDescription)).forEach(remedial -> {

                Map<String, String> detail = new HashMap<>();

                //II.2.1 Limit violations by remedial
                networkLimitViolationIdentifiers.clear();
                mapLimitViolationByContextAndLimitViolationIdentifier.clear();

                networkContexts.forEach(networkContext -> {

                    List<NetworkActionResult> networkActionResults = mapNetworkActionResultsByContextAndContingency.get(Pair.of(networkContext, networkContingency))
                            .stream().filter(networkActionResult -> networkActionResult.getRemedial() == remedial).collect(Collectors.toList());

                    if (!networkActionResults.isEmpty()) {

                        NetworkActionResult networkActionResult = networkActionResults.get(0);

                        //define remedial status
                        defineRemedialStatus(networkActionResult, detail);

                        List<NetworkLimitViolation> networkLimitViolations = networkActionResult.getVariantResult().getNetworkLimitViolationList();

                        prepareStructure(networkContext, networkLimitViolations, networkLimitViolationIdentifiers, mapLimitViolationByContextAndLimitViolationIdentifier);
                    }
                });

                List<CardNetworkLimitViolation> cardNetworkLimitViolationsPerRemedial = makeCardNetworkLimitViolations(networkLimitViolationIdentifiers, networkContexts, mapLimitViolationByContextAndLimitViolationIdentifier);

                cardNetworkRemedials.add(new CardNetworkRemedial(remedial.getShortDescription(), cardNetworkLimitViolationsPerRemedial, detail));
            });

            if (!cardNetworkLimitViolations.isEmpty() || !cardNetworkRemedials.isEmpty()) {
                cardNetworkContingencies.add(new CardNetworkContingency(networkContingency.getId(), cardNetworkLimitViolations, cardNetworkRemedials, null));
            }
        });
        return cardNetworkContingencies;
    }

    void defineRemedialStatus(NetworkActionResult networkActionResult, Map<String, String> detail) {

        if (!detail.containsKey("status")) {
            String status;

            if (networkActionResult.getVariantResult().isComputationOk()) {
                boolean hasViolations = !networkActionResult.isActionEfficient();
                status = hasViolations ? Status.V_R_AV.name() : Status.NO_V.name();
            } else {
                status = Status.V_R_CMP_NOK.name();
            }

            detail.put("status", status);
        }
    }

    private void prepareStructure(NetworkContext networkContext, List<NetworkLimitViolation> networkLimitViolations, Set<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiers, Map<Pair<NetworkContext, NetworkLimitViolationIdentifier>, NetworkLimitViolation> mapLimitViolationByContextAndLimitViolationIdentifier) {
        List<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiersTmp = networkLimitViolations.stream()
                .map(networkLimitViolation -> {
                    NetworkLimitViolationIdentifier networkLimitViolationIdentifier = new NetworkLimitViolationIdentifier(networkLimitViolation.getSubjectId(), networkLimitViolation.getLimitType(), networkLimitViolation.getAcceptableDuration(), networkLimitViolation.getSide());
                    mapLimitViolationByContextAndLimitViolationIdentifier.put(Pair.of(networkContext, networkLimitViolationIdentifier), networkLimitViolation);

                    return networkLimitViolationIdentifier;
                })
                .collect(Collectors.toList());

        networkLimitViolationIdentifiers.addAll(networkLimitViolationIdentifiersTmp);
    }

    private List<CardNetworkLimitViolation> makeCardNetworkLimitViolations(Set<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiers, List<NetworkContext> networkContexts, Map<Pair<NetworkContext, NetworkLimitViolationIdentifier>, NetworkLimitViolation> mapLimitViolationByContextAndLimitViolationIdentifier) {
        List<CardNetworkLimitViolation> cardNetworkLimitViolations = new ArrayList<>();

        List<NetworkLimitViolationIdentifier> networkLimitViolationIdentifiersSorted = networkLimitViolationIdentifiers.stream().sorted(Comparator.comparing(NetworkLimitViolationIdentifier::getSubjectId, Comparator.nullsLast(String::compareTo))
                .thenComparing(NetworkLimitViolationIdentifier::getLimitType, Comparator.nullsLast(String::compareTo))
                .thenComparing(NetworkLimitViolationIdentifier::getAcceptableDuration, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(NetworkLimitViolationIdentifier::getSide, Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());

        networkLimitViolationIdentifiersSorted.forEach(networkLimitViolationIdentifier -> cardNetworkLimitViolations.add(makeCardNetworkLimitViolation(networkLimitViolationIdentifier, networkContexts, mapLimitViolationByContextAndLimitViolationIdentifier, networkLimitViolationIdentifiersSorted.indexOf(networkLimitViolationIdentifier))));

        return cardNetworkLimitViolations;
    }

    private CardNetworkLimitViolation makeCardNetworkLimitViolation(NetworkLimitViolationIdentifier networkLimitViolationIdentifier, List<NetworkContext> networkContexts, Map<Pair<NetworkContext, NetworkLimitViolationIdentifier>, NetworkLimitViolation> mapNetworkLimitViolations, int index) {
        String name = networkLimitViolationIdentifier.getSubjectId();

        Map<String, String> detail = new HashMap<>();
        if (networkLimitViolationIdentifier.getLimitType() != null) {
            detail.put("limitType", networkLimitViolationIdentifier.getLimitType());
        }

        if (networkLimitViolationIdentifier.getAcceptableDuration() != null) {
            detail.put("acceptableDuration", networkLimitViolationIdentifier.getAcceptableDuration().toString());
        }

        if (networkLimitViolationIdentifier.getSide() != null) {
            detail.put("side", networkLimitViolationIdentifier.getSide());
        }

        List<CardNetworkLimitViolationValueItem> values = new ArrayList<>();

        CardNetworkLimitViolation cardNetworkLimitViolation = new CardNetworkLimitViolation(name, index == 0 ? makeCardNetworkContext(networkContexts) : Collections.emptyList(), values, detail);

        networkContexts.forEach(networkContext -> {
            String value = "";
            Map<String, String> valueDetail = null;

            if (mapNetworkLimitViolations.containsKey(Pair.of(networkContext, networkLimitViolationIdentifier))) {
                NetworkLimitViolation networkLimitViolation = mapNetworkLimitViolations.get(Pair.of(networkContext, networkLimitViolationIdentifier));
                value = String.format("%.0f", networkLimitViolation.getValue() / networkLimitViolation.getLimit() * 100) + "%";

                valueDetail = new HashMap<>();

                valueDetail.put("value", Double.toString(networkLimitViolation.getValue()));
                valueDetail.put("limit", Double.toString(networkLimitViolation.getLimit()));

                if (networkLimitViolation.getValueMw() != null) {
                    valueDetail.put("valueMw", Double.toString(networkLimitViolation.getValueMw()));
                }

                if (networkLimitViolation.getPreValue() != null) {
                    valueDetail.put("preValue", Double.toString(networkLimitViolation.getPreValue()));
                }

                if (networkLimitViolation.getPreValueMw() != null) {
                    valueDetail.put("preValueMw", Double.toString(networkLimitViolation.getPreValueMw()));
                }
            }

            CardNetworkLimitViolationValueItem valueItem = new CardNetworkLimitViolationValueItem(value, valueDetail);

            values.add(valueItem);
        });

        return cardNetworkLimitViolation;
    }

    private List<CardNetworkContext> makeCardNetworkContext(List<NetworkContext> networkContexts) {

        List<CardNetworkContext> cardNetworkContexts = new ArrayList<>();

        networkContexts.forEach(networkContext -> {
            Long date = networkContext.getNetworkDate().toEpochMilli();

            Map<String, Object> detail = new HashMap<>();
            detail.put("type", networkContext.getCaseType().getName());
            detail.put("computationDate", networkContext.getComputationDate().toEpochMilli());

            cardNetworkContexts.add(new CardNetworkContext(date, detail));
        });

        return cardNetworkContexts;
    }

    @Data
    private class GenericType {
        private String name;

        private String processId;

        private CaseType caseType;

        private boolean isCaseType;

        private TimerangeType timerangeType;

        private boolean isTimerangeType;

        private Integer startDateIncrement;

        private Integer endDateIncrement;

        public GenericType(CaseType caseType) {
            this.caseType = caseType;
            this.name = caseType.getName();
            this.processId = "CASETYPE";
            this.isCaseType = true;
            this.startDateIncrement = caseType.getCardStartDateIncrement();
            this.endDateIncrement = caseType.getCardEndDateIncrement();
        }

        public GenericType(TimerangeType timerangeType) {
            this.timerangeType = timerangeType;
            this.name = timerangeType.getName();
            this.processId = "TIMERANGE";
            this.isTimerangeType = true;
            this.startDateIncrement = timerangeType.getCardStartDateIncrement();
            this.endDateIncrement = timerangeType.getCardEndDateIncrement();
        }

        public String toString() {
            return this.isCaseType ? "caseType=" : "rangeTime=" + this.name;
        }
    }

}

