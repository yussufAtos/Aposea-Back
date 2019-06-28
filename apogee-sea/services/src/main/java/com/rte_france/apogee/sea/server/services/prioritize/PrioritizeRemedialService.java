package com.rte_france.apogee.sea.server.services.prioritize;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkPostContingencyResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.PrioritizeRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.RemedialRepository;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.PrioritizeRemedial;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.logic.PrioritizeRemedialByContingency;
import com.rte_france.apogee.sea.server.services.logic.RemedialIdentifier;
import com.rte_france.apogee.sea.server.services.logic.RemedialsListForPrioritize;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class PrioritizeRemedialService implements IPrioritizeRemedialsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizeRemedialService.class);

    private PrioritizeRepository prioritizeRepository;

    private RemedialRepository remedialRepository;

    private NetworkContingencyRepository networkContingencyRepository;

    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    @Getter
    @Setter
    @Value("${apogee.prioritize.maxNumPrioritizeRemedial:3}")
    private String maxNumPrioritizeRemedial;

    @Autowired
    PrioritizeRemedialService(PrioritizeRepository prioritizeRepository, RemedialRepository remedialRepository,
                              NetworkContingencyRepository networkContingencyRepository, NetworkPostContingencyResultRepository networkPostContingencyResultRepository) {
        this.prioritizeRepository = prioritizeRepository;
        this.remedialRepository = remedialRepository;
        this.networkContingencyRepository = networkContingencyRepository;
        this.networkPostContingencyResultRepository = networkPostContingencyResultRepository;
    }

    @Override
    public List<Remedial> getRemedials(String contingencyId) {
        List<Remedial> remedials = null;
        if ((contingencyId != null) && !"".equals(contingencyId)) {
            remedials = networkContingencyRepository.findAllRemedialsByContingency(contingencyId);
        } else {
            remedials = remedialRepository.findAll();
        }
        return remedials;
    }

    @Override
    public void deleteRemedials(String remedialsId) throws PrioritizeRemedialServiceException {
        try {
            if ((remedialsId != null) && !"".equals(remedialsId)) {
                Optional<Remedial> remedialsOptional = remedialRepository.findById(remedialsId);
                if (remedialsOptional.isPresent()) {
                    List<NetworkContingency> contingencies = networkContingencyRepository.findAllContingencyByRemedialId(remedialsId);
                    List<NetworkPostContingencyResult> postContingencyResults = networkPostContingencyResultRepository.findAllPostContingencyResultByRemedialId(remedialsId);
                    contingencies.forEach(networkContingency -> networkContingency.getAllRemedials().remove(remedialsOptional.get()));
                    postContingencyResults.forEach(networkPostContingencyResult -> networkPostContingencyResult.getRemedials().remove(remedialsOptional.get()));
                    networkPostContingencyResultRepository.saveAll(postContingencyResults);
                    networkContingencyRepository.saveAll(contingencies);
                    remedialRepository.delete(remedialsOptional.get());
                    LOGGER.info("PrioritizeRemedial  : delete remedial with id ={} in data base", remedialsId);
                }

            } else {
                List<NetworkContingency> contingencies = networkContingencyRepository.findAll();
                List<NetworkPostContingencyResult> postContingencyResults = networkPostContingencyResultRepository.findAll();
                contingencies.forEach(networkContingency -> networkContingency.getAllRemedials().clear());
                postContingencyResults.forEach(networkPostContingencyResult -> networkPostContingencyResult.getRemedials().clear());
                networkPostContingencyResultRepository.saveAll(postContingencyResults);
                networkContingencyRepository.saveAll(contingencies);
                remedialRepository.deleteAll();
                LOGGER.info("PrioritizeRemedial  : delete all remedials in data base");
            }
        } catch (IllegalArgumentException e) {
            throw new PrioritizeRemedialServiceException("Failed deleting remedial", e);
        }
    }

    @Override
    public List<Prioritize> getPrioritizeRemedial(String prioritizeDate, String contingencyId) throws PrioritizeRemedialServiceException {
        List<Prioritize> prioritizes = new ArrayList<>();
        try {
            if ((prioritizeDate != null) && !"".equals(prioritizeDate)) {
                Instant date = Instant.parse(prioritizeDate);
                if ((contingencyId != null) && !"".equals(contingencyId)) {
                    Optional<Prioritize> prioritizeOptional = prioritizeRepository.findLastPrioritizeRemedialByContingencyAndDate(date, contingencyId);
                    prioritizeOptional.ifPresent(prioritize -> prioritizes.add(prioritizeOptional.get()));
                } else {
                    prioritizes.addAll(prioritizeRepository.findLastPrioritizeRemedial(date));
                }
                return prioritizes;
            }
        } catch (DateTimeParseException e) {
            throw new PrioritizeRemedialServiceException("Failed parsing prioritizeDate", e);
        }
        return prioritizeRepository.findAll();
    }

    @Override
    public RemedialsListForPrioritize getRemedialsListForPrioritize(String prioritizeDate, String contingencyId, String contextId) throws PrioritizeRemedialServiceException {
        List<Prioritize> prioritizedRemedials = new ArrayList<>();
        RemedialsListForPrioritize remedialsListForPrioritize = new RemedialsListForPrioritize();
        try {
            if ((prioritizeDate != null) && !"".equals(prioritizeDate) && (contingencyId != null) && !"".equals(contingencyId) && (contextId != null) && !"".equals(contextId)) {
                Instant date = Instant.parse(prioritizeDate);
                Long contextIdLong = Long.parseLong(contextId);

                Optional<Prioritize> prioritizeOptional = prioritizeRepository.findLastPrioritizeRemedialByContingencyAndDate(date, contingencyId);
                prioritizeOptional.ifPresent(prioritize -> prioritizedRemedials.add(prioritizeOptional.get()));
                remedialsListForPrioritize.setPrioritizedRemedials(prioritizedRemedials);
                remedialsListForPrioritize.setAllRemedials(networkContingencyRepository.findAllRemedialsByContingency(contingencyId));
                remedialsListForPrioritize.setCandidatesRemedials(networkPostContingencyResultRepository.findRemedialsByNetworkContextAndNetworkContingency(contextIdLong, contingencyId));

            } else {
                throw new PrioritizeRemedialServiceException("Failed parameters request");
            }
        } catch (DateTimeParseException e) {
            throw new PrioritizeRemedialServiceException("Parse date is failed", e);
        } catch (NumberFormatException e) {
            throw new PrioritizeRemedialServiceException("Number format of networkcontext is failed", e);
        }
        return remedialsListForPrioritize;
    }


    @Override
    public void deleteByNetworkContingency(String contingencyId) {
        if ((contingencyId != null) && !"".equals(contingencyId)) {
            List<Prioritize> prioritizeList = prioritizeRepository.findByNetworkContingency(contingencyId);
            if (!prioritizeList.isEmpty()) {
                prioritizeRepository.deleteAll(prioritizeList);
                LOGGER.info("PrioritizeRemedial  : delete all prioritizes for contingency ={} in data base", contingencyId);
            } else {
                LOGGER.info("PrioritizeRemedial  : don't have prioritizes for contingency ={} in data base", contingencyId);
            }
        } else {
            prioritizeRepository.deleteAll();
            LOGGER.info("PrioritizeRemedial  : delete all prioritizes in data base");
        }
    }

    @Override
    public Map<String, List<RemedialIdentifier>> findRemedialsPrioritizeByNetworkDate(Instant networkDate) {
        List<NetworkContingency> networkContingencyList = networkContingencyRepository.findAll();
        Map<String, List<RemedialIdentifier>> prioritizeRemedialMap = new HashMap();
        for (NetworkContingency networkContingency : networkContingencyList) {
            List<RemedialIdentifier> remedialIdentifierList = new ArrayList<>();
            Optional<Prioritize> lastPrioritizeOptional = prioritizeRepository.findLastPrioritizeRemedialByContingencyAndDate(networkDate, networkContingency.getId());
            Prioritize lastPrioritize = null;
            if (lastPrioritizeOptional.isPresent()) {
                lastPrioritize = lastPrioritizeOptional.get();
                if (!lastPrioritize.getPrioritizeRemedialList().isEmpty()) {
                    lastPrioritize.getPrioritizeRemedialList().forEach(prioritizeRemedial -> {
                        Optional<Remedial> remedialOptional = remedialRepository.findById(prioritizeRemedial.getRemedial().getIdRemedialRepository());
                        if (remedialOptional.isPresent()) {
                            Remedial remedial = remedialOptional.get();
                            RemedialIdentifier remedialIdentifier = new RemedialIdentifier();
                            remedialIdentifier.setIdAbstractLogic(remedial.getIdRemedialRepository());
                            remedialIdentifier.setShortDescription(remedial.getShortDescription());
                            remedialIdentifier.setIdLogicContext(remedial.getIdLogicContext());
                            remedialIdentifierList.add(remedialIdentifier);
                        }
                    });
                    prioritizeRemedialMap.put(networkContingency.getId(), remedialIdentifierList);
                }
            }
        }
        return prioritizeRemedialMap;
    }

    @Override
    @Transactional
    public void savePrioritizeRemedial(List<Prioritize> prioritizeRemedial) throws PrioritizeRemedialServiceException {
        if (prioritizeRemedial != null && !prioritizeRemedial.isEmpty()) {
            for (Prioritize prioritize : prioritizeRemedial) {
                PrioritizeRemedialByContingency prioritizeRemedialByContingency = new PrioritizeRemedialByContingency();
                prioritizeRemedialByContingency.setContingency(prioritize.getNetworkContingency());
                prioritizeRemedialByContingency.setStartDate(prioritize.getPrioritizeStartDate().toString());
                if (prioritize.getPrioritizeEndDate() != null) {
                    prioritizeRemedialByContingency.setEndDate(prioritize.getPrioritizeEndDate().toString());
                }
                prioritizeRemedialByContingency.setPrioritizedRemedials(prioritize.getPrioritizeRemedialList());

                try {
                    saveOnePrioritizeRemedial(prioritizeRemedialByContingency);
                } catch (PrioritizeRemedialServiceException e) {
                    throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: Error while saving the prioritize remedials", e);
                }
            }
        } else {
            throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: The list of prioritize must be not-empty");
        }
    }

    private void saveOnePrioritizeRemedial(PrioritizeRemedialByContingency actualPrioritizeRemedial) throws PrioritizeRemedialServiceException {
        String contingencyId = null;
        if (actualPrioritizeRemedial.getContingency() != null && !actualPrioritizeRemedial.getContingency().getId().isEmpty() && !"".equals(actualPrioritizeRemedial.getContingency().getId())) {
            contingencyId = actualPrioritizeRemedial.getContingency().getId();
        } else {
            throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: The network contingency must be not-empty");
        }
        if (actualPrioritizeRemedial.getPrioritizedRemedials().isEmpty()) {
            throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: The list of remedials for prioritize must be not-empty");
        }
        String endDateDtring = actualPrioritizeRemedial.getEndDate();
        Instant startDate = null;
        Instant endDate = null;
        try {
            startDate = Instant.parse(actualPrioritizeRemedial.getStartDate());
            NetworkContingency networkContingency = getNetworkContingency(actualPrioritizeRemedial);
            if (endDateDtring != null) {
                endDate = Instant.parse(endDateDtring);
            }
            Optional<Prioritize> prioritizeOptional = prioritizeRepository.findByContingencyAndStartDateAndEndDate(contingencyId, startDate, endDate);
            if (!prioritizeOptional.isPresent()) {
                saveOnDatabase(actualPrioritizeRemedial, networkContingency, startDate, endDate);
            } else {
                prioritizeRepository.delete(prioritizeOptional.get());
                prioritizeRepository.flush();
                saveActualPrioritize(actualPrioritizeRemedial, networkContingency, startDate);
            }

        } catch (DateTimeParseException e) {
            throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: Error while parsing the start date for prioritize", e);
        } catch (PrioritizeRemedialServiceException e) {
            throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: Error while saving the prioritize remedials", e);
        }

    }


    private void saveOnDatabase(PrioritizeRemedialByContingency actualPrioritizeRemedial, NetworkContingency networkContingency, Instant startDate, Instant endDateNullable) throws PrioritizeRemedialServiceException {
        Instant endDate;
        try {
            if (endDateNullable == null) {
                endDate = Instant.MAX;
            } else {
                endDate = endDateNullable;
            }
            List<Prioritize> lastPrioritizeRemedialList = prioritizeRepository.findLastPrioritizeRemedialByContingency(actualPrioritizeRemedial.getContingency().getId());
            saveActualPrioritize(actualPrioritizeRemedial, networkContingency, startDate);
            for (Prioritize lastPrioritize : lastPrioritizeRemedialList) {
                String statusWithLastPrioritize = getStatusWithLastPrioritize(startDate, endDate, lastPrioritize);
                switch (statusWithLastPrioritize) {
                    case "right":
                        processingRight(lastPrioritize, startDate);
                        break;
                    case "left":
                        processingLeft(lastPrioritize, endDate);
                        break;
                    case "inSide":
                        processingInSide(lastPrioritize, startDate, endDate, networkContingency);
                        break;
                    case "outSide":
                        processingOutSide(lastPrioritize);
                        break;
                    default:
                        break;
                }
            }
        } catch (DateTimeParseException e) {
            throw new PrioritizeRemedialServiceException("Failed parsing prioritizeDate on prioritize remedials json", e);
        }
    }

    private void saveActualPrioritize(PrioritizeRemedialByContingency actualPrioritizeRemedial, NetworkContingency networkContingency, Instant startDate) throws PrioritizeRemedialServiceException {
        int prioritizeRemedialValue = 1;
        int maxNum = Integer.parseInt(maxNumPrioritizeRemedial);
        List<PrioritizeRemedial> prioritizeRemedials = new ArrayList<>();
        List<Remedial> remedials = new ArrayList<>();
        Prioritize prioritize = new Prioritize(startDate);
        String actualEndDateString = actualPrioritizeRemedial.getEndDate();
        Instant actualEndDate = null;
        if (actualEndDateString != null) {
            actualEndDate = Instant.parse(actualPrioritizeRemedial.getEndDate());
        }
        prioritize.setPrioritizeEndDate(actualEndDate);
        prioritize.setNetworkContingency(networkContingency);
        prioritize = prioritizeRepository.save(prioritize);
        prioritize.setPrioritizeRemedialList(prioritizeRemedials);

        for (PrioritizeRemedial prioritizeRemedial : actualPrioritizeRemedial.getPrioritizedRemedials()) {
            if (prioritizeRemedial.getRemedial() == null || prioritizeRemedial.getRemedial().getIdRemedialRepository().isEmpty() || "".equals(prioritizeRemedial.getRemedial().getIdRemedialRepository())) {
                throw new PrioritizeRemedialServiceException("PrioritizeRemedialService: The remedial must be not-empty");
            }
            if (maxNum >= prioritizeRemedialValue) {
                Optional<Remedial> remedialOptional = remedialRepository.findById(prioritizeRemedial.getRemedial().getIdRemedialRepository());
                Remedial remedial;
                if (remedialOptional.isPresent()) {
                    remedial = remedialOptional.get();
                    networkContingency.getAllRemedials().add(remedial);
                } else {
                    remedial = new Remedial(prioritizeRemedial.getRemedial().getIdRemedialRepository(), prioritizeRemedial.getRemedial().getShortDescription());
                    remedial.setIdLogicContext(prioritizeRemedial.getRemedial().getIdLogicContext());
                    remedials.add(remedial);
                }
                PrioritizeRemedial prioritizeRemedialCreate = new PrioritizeRemedial(prioritizeRemedialValue, prioritize);
                prioritizeRemedialCreate.setRemedial(remedial);
                prioritizeRemedials.add(prioritizeRemedialCreate);
                prioritizeRemedialValue = prioritizeRemedialValue + 1;
            }
        }
        if (!remedials.isEmpty()) {
            remedialRepository.saveAll(remedials);
            networkContingency.getAllRemedials().addAll(remedials);
        }
        networkContingencyRepository.save(networkContingency);
        prioritizeRepository.save(prioritize);
        LOGGER.info("Insert PrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", networkContingency.getId(), startDate, actualEndDate);
    }

    private void processingRight(Prioritize lastPrioritize, Instant startDate) {
        lastPrioritize.setPrioritizeEndDate(startDate);
        prioritizeRepository.saveAndFlush(lastPrioritize);
        LOGGER.info("update PrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", lastPrioritize.getNetworkContingency().getId(), lastPrioritize.getPrioritizeStartDate(), startDate);
    }

    private void processingLeft(Prioritize lastPrioritize, Instant endDate) {
        lastPrioritize.setPrioritizeStartDate(endDate);
        prioritizeRepository.saveAndFlush(lastPrioritize);
        LOGGER.info("updatePrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", lastPrioritize.getNetworkContingency().getId(), endDate, lastPrioritize.getPrioritizeEndDate());
    }

    private void processingInSide(Prioritize lastPrioritize, Instant startDate, Instant endDate, NetworkContingency networkContingency) {
        List<PrioritizeRemedial> prioritizeRemedials = new ArrayList<>();
        Prioritize prioritize = new Prioritize(endDate);
        prioritize.setPrioritizeEndDate(lastPrioritize.getPrioritizeEndDate());
        prioritize.setNetworkContingency(networkContingency);
        prioritize = prioritizeRepository.save(prioritize);
        for (PrioritizeRemedial lastPrioritizeRemedial : lastPrioritize.getPrioritizeRemedialList()) {
            PrioritizeRemedial prioritizeRemedial = new PrioritizeRemedial(lastPrioritizeRemedial.getPrioritizeValue(), prioritize);
            prioritizeRemedial.setRemedial(lastPrioritizeRemedial.getRemedial());
            prioritizeRemedials.add(prioritizeRemedial);
        }
        prioritize.setPrioritizeRemedialList(prioritizeRemedials);
        prioritizeRepository.save(prioritize);
        LOGGER.info("InsertPrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", networkContingency.getId(), endDate, lastPrioritize.getPrioritizeEndDate());

        lastPrioritize.setPrioritizeEndDate(startDate);
        prioritizeRepository.saveAndFlush(lastPrioritize);
        LOGGER.info("updatePrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", lastPrioritize.getNetworkContingency().getId(), lastPrioritize.getPrioritizeStartDate(), startDate);
    }

    private void processingOutSide(Prioritize lastPrioritize) {
        Instant startDate = lastPrioritize.getPrioritizeEndDate();
        Instant endDate = lastPrioritize.getPrioritizeEndDate();
        String contingencyId = lastPrioritize.getNetworkContingency().getId();
        prioritizeRepository.delete(lastPrioritize);
        LOGGER.info("InsertPrioritizeRemedial for : contingency ={}, startDate ={}, endDate ={} ", startDate, endDate, contingencyId);
    }

    private String getStatusWithLastPrioritize(Instant startDate, Instant endDate, Prioritize lastPrioritize) {
        Instant lastEndDate = lastPrioritize.getPrioritizeEndDate();
        if (lastEndDate == null) {
            lastEndDate = Instant.MAX;
        }
        if ((lastPrioritize.getPrioritizeStartDate().isBefore(startDate)) && (lastEndDate.isBefore(endDate) || lastEndDate.equals(endDate)) && (lastEndDate.isAfter(startDate))) {
            return "right";
        }
        if ((lastPrioritize.getPrioritizeStartDate().isAfter(startDate) || lastPrioritize.getPrioritizeStartDate().equals(startDate)) && (lastEndDate.isAfter(endDate)) && (lastPrioritize.getPrioritizeStartDate().isBefore(endDate))) {
            return "left";
        }
        if ((lastPrioritize.getPrioritizeStartDate().isBefore(startDate)) && (lastEndDate.isAfter(endDate))) {
            return "inSide";
        }
        if ((lastPrioritize.getPrioritizeStartDate().isAfter(startDate) || lastPrioritize.getPrioritizeStartDate().equals(startDate)) && (lastEndDate.isBefore(endDate) || lastEndDate.equals(endDate))) {
            return "outSide";
        }
        return "false";
    }

    private NetworkContingency getNetworkContingency(PrioritizeRemedialByContingency actualPrioritizeRemedial) {
        NetworkContingency networkContingency;
        Optional<NetworkContingency> networkContingencyOptional = networkContingencyRepository.findById(actualPrioritizeRemedial.getContingency().getId());
        if (networkContingencyOptional.isPresent()) {
            networkContingency = networkContingencyOptional.get();
        } else {
            networkContingency = new NetworkContingency(actualPrioritizeRemedial.getContingency().getId());
            networkContingency.setNetworkContingencyElementList(actualPrioritizeRemedial.getContingency().getNetworkContingencyElementList());
            networkContingency = networkContingencyRepository.saveAndFlush(networkContingency);
        }
        return networkContingency;
    }
}
