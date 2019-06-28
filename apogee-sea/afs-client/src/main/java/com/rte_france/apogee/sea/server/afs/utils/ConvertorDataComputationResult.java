package com.rte_france.apogee.sea.server.afs.utils;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.extensions.ActivePowerExtension;
import com.powsybl.security.extensions.CurrentExtension;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.computation.variant.NetworkActionResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.RemedialRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkElementRepository;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.model.zones.NetworkElement;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.itesla.variant.result.*;
import com.rte_france.powsybl.hades2.extensions.VoltageExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@PropertySource("classpath:apogee.properties")
public class ConvertorDataComputationResult {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvertorDataComputationResult.class);

    private NetworkContingencyRepository networkContingencyRepository;

    private VoltageConstraintsFilter voltageConstraintsFilter;

    private NetworkElementRepository networkElementRepository;

    private RemedialRepository remedialRepository;

    @Autowired
    public ConvertorDataComputationResult(NetworkContingencyRepository networkContingencyRepository,
                                          NetworkElementRepository networkElementRepository, RemedialRepository remedialRepository,
                                          @Value("${apogee.voltageConstraints.filter:BASECASE_ONLY}") String voltageConstraintsFilter) {
        this.networkContingencyRepository = networkContingencyRepository;
        this.networkElementRepository = networkElementRepository;
        this.remedialRepository = remedialRepository;
        this.voltageConstraintsFilter = VoltageConstraintsFilter.valueOf(voltageConstraintsFilter);
    }

    /**
     * @param networkLimitViolationsResult network limit violation result.
     * @param limitViolationsResult        The iTesla limit violations result.
     */
    public void populateNetworkLimitViolationsResult(NetworkLimitViolationsResult networkLimitViolationsResult, LimitViolationsResult limitViolationsResult, boolean baseCase) {
        networkLimitViolationsResult.setComputationOk(limitViolationsResult.isComputationOk());
        limitViolationsResult.getLimitViolations().forEach(limitViolation -> {
            NetworkLimitViolation networkLimitViolation = setLimitViolationINNetworkLimitViolation(baseCase, limitViolation);
            if (networkLimitViolation == null) {
                return;
            }

            networkLimitViolation.setNetworkLimitViolationsResult(networkLimitViolationsResult);
            networkLimitViolationsResult.getNetworkLimitViolationList().add(networkLimitViolation);
        });

    }

    private NetworkLimitViolation setLimitViolationINNetworkLimitViolation(boolean baseCase, LimitViolation limitViolation) {
        //is it a Voltage limit violation ?
        final boolean isVoltageLimitViolation = limitViolation.getLimitType() != null &&
                (limitViolation.getLimitType() == LimitViolationType.HIGH_VOLTAGE ||
                        limitViolation.getLimitType() == LimitViolationType.LOW_VOLTAGE);

        /*
         * For Voltage Constraints (limitType = HIGH_VOLTAGE and LOW_VOLTAGE)
         * VoltageConstraintsFilter.NONE: do not store any constraint
         * VoltageConstraintsFilter.BASECASE_ONLY: Store only basecase constraints
         * */
        if (isVoltageLimitViolation &&
                (voltageConstraintsFilter == VoltageConstraintsFilter.NONE ||
                        (voltageConstraintsFilter == VoltageConstraintsFilter.BASECASE_ONLY && !baseCase))) {
            return null;
        }

        NetworkLimitViolation networkLimitViolation = new NetworkLimitViolation();
        networkLimitViolation.setSubjectId(limitViolation.getSubjectId());

        //TODO: replace with iTesla inputs once available
        Optional<NetworkElement> one = networkElementRepository.findByObjectid(limitViolation.getSubjectId());
        if (one.isPresent()) {
            Set<NetworkVoltageLevel> voltageLevels = new HashSet<>();
            voltageLevels.addAll(one.get().getNetworkVoltageLevels());
            networkLimitViolation.setNetworkVoltageLevels(voltageLevels);
        } else {
            LOGGER.warn("No voltageLevel found for limitViolation with subjectId={}", limitViolation.getSubjectId());
        }

        if (limitViolation.getLimitType() != null) {
            setExtensions(baseCase, limitViolation, networkLimitViolation);
            networkLimitViolation.setLimitType(limitViolation.getLimitType().name());
        }
        networkLimitViolation.setLimit(limitViolation.getLimit());
        networkLimitViolation.setLimitName(limitViolation.getLimitName());
        if (Integer.MAX_VALUE == limitViolation.getAcceptableDuration()) {
            networkLimitViolation.setAcceptableDuration(null);
        } else {
            networkLimitViolation.setAcceptableDuration(limitViolation.getAcceptableDuration());
        }

        networkLimitViolation.setLimitReduction(limitViolation.getLimitReduction());
        networkLimitViolation.setValue(limitViolation.getValue());
        if (limitViolation.getSide() != null) {
            networkLimitViolation.setSide(limitViolation.getSide().name());
        }
        return networkLimitViolation;
    }

    private void setExtensions(boolean baseCase, LimitViolation limitViolation, NetworkLimitViolation networkLimitViolation) {
        limitViolation.getExtensions().forEach(limitViolationExtension -> {
            if (limitViolationExtension instanceof VoltageExtension) {
                networkLimitViolation.setPreValue((double) ((VoltageExtension) limitViolationExtension).getPreContingencyValue());
            }

            if (limitViolationExtension instanceof CurrentExtension) {
                networkLimitViolation.setPreValue(((CurrentExtension) limitViolationExtension).getPreContingencyValue());
            }

            if (limitViolationExtension instanceof ActivePowerExtension) {
                if (baseCase) {
                    networkLimitViolation.setValueMw(((ActivePowerExtension) limitViolationExtension).getPreContingencyValue());
                } else {
                    networkLimitViolation.setPreValueMw(((ActivePowerExtension) limitViolationExtension).getPreContingencyValue());
                    networkLimitViolation.setValueMw(((ActivePowerExtension) limitViolationExtension).getPostContingencyValue());
                }
            }

        });
    }

    /**
     * <p>Create network post contingency result with the iTesla post contingency result</p>
     *
     * @param postContingencyResultList The iTesla Post contingency result list
     * @return Network Post contingency result list
     */
    public List<NetworkPostContingencyResult> createNetworkPostContingencyResultList(List<PostContingencyResult> postContingencyResultList) {
        List<NetworkPostContingencyResult> networkPostContingencyResultList = new ArrayList<>();
        postContingencyResultList.forEach(postContingencyResult -> {
            Optional<NetworkPostContingencyResult> networkPostContingencyResultOptional = createNetworkPostContingencyResult(postContingencyResult);
            networkPostContingencyResultOptional.ifPresent(networkPostContingencyResult -> networkPostContingencyResultList.add(networkPostContingencyResultOptional.get()));
        });
        return networkPostContingencyResultList;
    }


    /**
     * <p>Populate post contingency result list in network security result</p>
     *
     * @param networkSecurityAnalysisResult Network security analysis result
     * @param postContingencyResultList     The iTesla Post contingency result list
     */
    public void populateNetworkPostContingencyResultList(NetworkSecurityAnalysisResult networkSecurityAnalysisResult, List<PostContingencyResult> postContingencyResultList) {
        List<NetworkPostContingencyResult> networkPostContingencyResultList = networkSecurityAnalysisResult.getPostContingencyResults();
        postContingencyResultList.forEach(postContingencyResult -> {
            Optional<NetworkPostContingencyResult> networkPostContingencyResultOptional = createNetworkPostContingencyResult(postContingencyResult);
            networkPostContingencyResultOptional.ifPresent(networkPostContingencyResult -> {
                networkPostContingencyResultList.add(networkPostContingencyResult);
                networkPostContingencyResult.setComputationResult(networkSecurityAnalysisResult);
            });
        });
    }


    /**
     * * <p>Populate network contingency</p>
     *
     * @param contingency        The iTesla contingency.
     * @param networkContingency o create or to update in data base.
     *                           Its value can be null in the case where the query in the database with the id of itesla contingency returns null.
     *                           In the case where it is not null, it is not associated with any voltage level
     * @return The network contingency created with the iTesla contingency.
     */
    private NetworkContingency createNetworkContingencyWithNetWorkContingencyElement(Contingency contingency, NetworkContingency networkContingency) {

        List<NetworkContingencyElement> networkContingencyElementlist = new ArrayList<>();
        Set<NetworkVoltageLevel> networkVoltageLevelSet = new HashSet<>();
        NetworkContingency networkContingencyCreated;

        if (networkContingency == null) {
            networkContingencyCreated = new NetworkContingency(contingency.getId());
        } else {
            networkContingencyCreated = networkContingency;
        }
        for (ContingencyElement contingencyElement : contingency.getElements()) {
            NetworkContingencyElement networkContingencyElement = new NetworkContingencyElement();
            networkContingencyElement.setContingencyElementType(contingencyElement.getType());
            networkContingencyElement.setEquipmentName(contingencyElement.getId());

            networkContingencyElement.setNetworkContingency(networkContingencyCreated);
            networkContingencyElementlist.add(networkContingencyElement);
            Optional<NetworkElement> one = networkElementRepository.findByObjectid(contingencyElement.getId());
            if (one.isPresent()) {
                networkVoltageLevelSet.addAll(one.get().getNetworkVoltageLevels());
            } else {
                LOGGER.warn("No voltageLevel found for NetworkContingency / NetworkContingencyElement {} / {}", contingency.getId(), contingencyElement.getId());
            }
        }
        networkContingencyCreated.setNetworkContingencyElementList(networkContingencyElementlist);
        //TODO: replace with iTesla inputs once available
        networkContingencyCreated.setNetworkVoltageLevels(networkVoltageLevelSet);

        return networkContingencyCreated;
    }


    /**
     * <p>Set post contingency result of the iTesla security analysis on post contingency result of the model.</p>
     *
     * @param postContingencyResult The iTesla post contingency result.
     * @return The network post contingency result created with the iTesla post contingency result.
     */
    private Optional<NetworkPostContingencyResult> createNetworkPostContingencyResult(PostContingencyResult postContingencyResult) {
        LimitViolationsResult limitViolationsResult = postContingencyResult.getLimitViolationsResult();

        if (!limitViolationsResult.isComputationOk() || !limitViolationsResult.getLimitViolations().isEmpty()) {
            NetworkPostContingencyResult networkPostContingencyResult = new NetworkPostContingencyResult();
            Optional<NetworkContingency> networkContingencyOptional = networkContingencyRepository.findById(postContingencyResult.getContingency().getId());
            NetworkContingency networkContingency = null;

            networkContingency = createNetworkContingency(networkContingencyOptional, postContingencyResult.getContingency());

            networkPostContingencyResult.setNetworkContingency(networkContingency);
            NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(postContingencyResult.getLimitViolationsResult().isComputationOk(), new ArrayList<>());
            populateNetworkLimitViolationsResult(networkLimitViolationsResult, postContingencyResult.getLimitViolationsResult(), false);
            if (networkLimitViolationsResult.isComputationOk() && networkLimitViolationsResult.getNetworkLimitViolationList().isEmpty()) {
                return Optional.empty();
            }
            networkPostContingencyResult.setNetworkLimitViolationsResult(networkLimitViolationsResult);
            return Optional.of(networkPostContingencyResult);
        } else {
            return Optional.empty();
        }
    }

    private void populateActionsResultInLimitViolationResult(NetworkLimitViolationsResult networkLimitViolationsResult, List<ActionResult> actionsResults, boolean baseCase) {
        List<NetworkActionResult> networkActionResults = networkLimitViolationsResult.getActionsResults();

        actionsResults.forEach(actionResult -> {
            NetworkActionResult networkActionResult = new NetworkActionResult();

            NetworkLimitViolationsResult variant = new NetworkLimitViolationsResult();
            LimitViolationsResult limitViolationsResult = new LimitViolationsResult(actionResult.getVariantResult().isComputationOk(), actionResult.getVariantResult().getViolations());
            populateNetworkLimitViolationsResult(variant, limitViolationsResult, baseCase);
            networkActionResult.setVariantResult(variant);
            networkActionResult.setActionEfficient(actionResult.isActionEfficient());
            int first = actionResult.getRuleId().indexOf('.');
            int second = actionResult.getRuleId().indexOf('.', first + 1);
            String remedialId = actionResult.getRuleId().substring(first + 1, second);
            Optional<Remedial> remedialOptional = remedialRepository.findById(remedialId);
            Remedial remedial = null;
            if (!remedialOptional.isPresent()) {
                remedial = new Remedial(remedialId, remedialId);
                remedial.setIdLogicContext(remedialId);
                remedial = remedialRepository.save(remedial);
            } else {
                remedial = remedialOptional.get();
            }
            networkActionResult.setRemedial(remedial);
            networkActionResult.setNetworkLimitViolationsResult(networkLimitViolationsResult);
            networkActionResults.add(networkActionResult);
        });
    }

    private NetworkLimitViolationsResult populateLimitViolationResult(StateResult stateResult, boolean baseCase) {
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult();
        LimitViolationsResult limitViolationsResult = new LimitViolationsResult(stateResult.getInitialVariant().isComputationOk(), stateResult.getInitialVariant().getViolations());
        populateNetworkLimitViolationsResult(networkLimitViolationsResult, limitViolationsResult, baseCase);

        List<ActionResult> actionsResults = stateResult.getActionsResults();
        populateActionsResultInLimitViolationResult(networkLimitViolationsResult, actionsResults, baseCase);
        return networkLimitViolationsResult;
    }

    /**
     * <p>Set post contingency variant result of the iTesla security analysis on post contingency result of the model.</p>
     *
     * @param postContingencyVariantsResult The iTesla post contingency variant result.
     * @return The network post contingency result created with the iTesla post contingency variants result.
     */
    private Optional<NetworkPostContingencyResult> createNetworkPostContingencyResult(PostContingencyVariantsResult postContingencyVariantsResult) {
        VariantResult variantResult = postContingencyVariantsResult.getResults().getInitialVariant();

        if (!variantResult.isComputationOk() || !variantResult.getViolations().isEmpty()) {
            NetworkPostContingencyResult networkPostContingencyResult = new NetworkPostContingencyResult();
            Optional<NetworkContingency> networkContingencyOptional = networkContingencyRepository.findById(postContingencyVariantsResult.getContingency().getId());
            NetworkContingency networkContingency = null;

            networkContingency = createNetworkContingency(networkContingencyOptional, postContingencyVariantsResult.getContingency());
            networkPostContingencyResult.setNetworkContingency(networkContingency);

            NetworkLimitViolationsResult networkStateResult = populateLimitViolationResult(postContingencyVariantsResult.getResults(), false);


            if (networkStateResult.isComputationOk() && networkStateResult.getNetworkLimitViolationList().isEmpty()) {
                return Optional.empty();
            }
            networkPostContingencyResult.setNetworkLimitViolationsResult(networkStateResult);
            return Optional.of(networkPostContingencyResult);

        } else {
            return Optional.empty();
        }
    }

    private NetworkContingency createNetworkContingency(Optional<NetworkContingency> networkContingencyOptional, Contingency contingency) {
        NetworkContingency networkContingency = null;

        if (networkContingencyOptional.isPresent() && !networkContingencyOptional.get().getNetworkVoltageLevels().isEmpty()) {
            networkContingency = networkContingencyOptional.get();
        } else {
            if (networkContingencyOptional.isPresent()) {
                networkContingency = networkContingencyOptional.get();
            }
            networkContingency = createNetworkContingencyWithNetWorkContingencyElement(contingency, networkContingency);
            networkContingency = networkContingencyRepository.save(networkContingency);
        }
        return networkContingency;
    }

    /**
     * <p>Populate post contingency variant result list in network security analysis result</p>
     *
     * @param networkSecurityAnalysisResult     network security analysis result
     * @param postContingencyVariantsResultList The iTesla Post contingency variants result list
     */
    private void populateNetworkPostContingencyVariantResultList(NetworkSecurityAnalysisResult networkSecurityAnalysisResult, List<PostContingencyVariantsResult> postContingencyVariantsResultList) {
        postContingencyVariantsResultList.forEach(postContingencyVariantsResult -> {
            Optional<NetworkPostContingencyResult> networkPostContingencyResultOptional = createNetworkPostContingencyResult(postContingencyVariantsResult);
            networkPostContingencyResultOptional.ifPresent(postContingencyVariantResult -> {
                networkSecurityAnalysisResult.getPostContingencyResults().add(postContingencyVariantResult);
                postContingencyVariantResult.setComputationResult(networkSecurityAnalysisResult);
            });
        });
    }

    /**
     * <p>Create network post contingency result with the iTesla post contingency variants result</p>
     *
     * @param postContingencyVariantsResults The iTesla Post contingency variants result list
     * @return Network Post contingency result list
     */
    public List<NetworkPostContingencyResult> createNetworkPostContingencyVariantResultList(List<PostContingencyVariantsResult> postContingencyVariantsResults) {
        List<NetworkPostContingencyResult> networkPostContingencyVariantsResultList = new ArrayList<>();
        postContingencyVariantsResults.forEach(postContingencyVariantResult -> {
            Optional<NetworkPostContingencyResult> networkPostContingencyVraiantsResultOptional = createNetworkPostContingencyResult(postContingencyVariantResult);
            networkPostContingencyVraiantsResultOptional.ifPresent(networkPostContingencyVariantsResult -> networkPostContingencyVariantsResultList.add(networkPostContingencyVraiantsResultOptional.get()));
        });
        return networkPostContingencyVariantsResultList;
    }


    public void populateNetworkVariantSimulatorResult(NetworkSecurityAnalysisResult networkSecurityAnalysisResult, VariantSimulatorResult variantSimulatorResult) {
        List<PostContingencyVariantsResult> postContingencyVariantsResults = variantSimulatorResult.getPostContingencyResults();
        StateResult stateResult = variantSimulatorResult.getPreContingencyResult();

        populateNetworkPostContingencyVariantResultList(networkSecurityAnalysisResult, postContingencyVariantsResults);
        NetworkLimitViolationsResult networkLimitViolationsResult = populateLimitViolationResult(stateResult, true);
        networkSecurityAnalysisResult.setPreContingencyResult(networkLimitViolationsResult);
    }
}
