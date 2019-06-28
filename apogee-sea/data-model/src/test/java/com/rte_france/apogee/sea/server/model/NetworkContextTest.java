package com.rte_france.apogee.sea.server.model;

import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Branch;
import com.powsybl.security.LimitViolationType;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {NetworkContextTest.class})
@EnableAutoConfiguration
@Transactional
public class NetworkContextTest {

    @Autowired
    private NetworkContingencyRepository networkContingencyRepository;
    @Autowired
    private NetworkContextRepository networkContextRepository;
    @Autowired
    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;
    @Autowired
    private NetworkSecurityAnalysisResultRepository networkSecurityAnalysisResultRepository;

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    NetworkLimitViolationsResult preContingencyResult;
    List<NetworkPostContingencyResult> postContingencyResults;
    NetworkContext networkContext;

    @BeforeEach
    public void setUp() {
        preContingencyResult = new NetworkLimitViolationsResult(true, Collections.emptyList());
        postContingencyResults = new ArrayList<NetworkPostContingencyResult>();
        networkContext = createNetworkContext();

        NetworkContingency networkContingency = networkContingencyRepository.save(createNetworkContingency());
        List<NetworkLimitViolation> limitViolations = createLimitViolations();
        NetworkPostContingencyResult postContingencyResult = new NetworkPostContingencyResult(networkContingency, true, limitViolations);
        postContingencyResults.add(postContingencyResult);
    }

    @AfterEach
    public void setDown() {

    }

    @Test
    public void test() {
        //Resultat en N-k

        NetworkSecurityAnalysisResult result = new NetworkSecurityAnalysisResult(preContingencyResult, postContingencyResults, networkContext);

        List<AbstractComputationResult> computationResultList = new ArrayList<>();
        computationResultList.add(result);
        networkContext.setComputationResultList(computationResultList);
        //save to DB
        networkContextRepository.save(networkContext);
        Long foundNetworkContext = networkContextRepository.count();
        assertThat(foundNetworkContext)
                .isNotNull();

        List<NetworkContext> networkContextList = networkContextRepository.findAll();
        networkContextList.forEach(networkContext1 -> {
            assertThat(networkContext1.getCaseType().getName())
                    .isNotNull()
                    .isEqualTo("srj-ij");

        });
    }

    @Test
    public void test1() {
        //Resultat en N
        List<NetworkContingencyElement> elements = new ArrayList<>();
        elements.add(createNetworkContingencyElement());
        NetworkContingency networkContingency = networkContingencyRepository.save(new NetworkContingency("contingency1", elements, postContingencyResults));

        NetworkSecurityAnalysisResult result1 = new NetworkSecurityAnalysisResult(preContingencyResult, networkContext);
        result1.getPostContingencyResults().forEach(networkPostContingencyResult -> {
            networkPostContingencyResult.setNetworkContingency(networkContingency);
        });

        List<AbstractComputationResult> computationResultList1 = new ArrayList<>();
        computationResultList1.add(result1);
        networkContext.setComputationResultList(computationResultList1);

        //save to DB
        networkContextRepository.save(networkContext);
        Long foundNetworkContextCont = networkContextRepository.count();
        assertThat(foundNetworkContextCont)
                .isNotNull();
        List<NetworkPostContingencyResult> networkPostContingencyResult = networkPostContingencyResultRepository.findAll();
        assertThat(networkPostContingencyResult.stream())
                .isEmpty();
    }

    @Test
    public void test2() {
        //Resultat en N-K without preContingencyResult
        NetworkSecurityAnalysisResult result2 = new NetworkSecurityAnalysisResult(postContingencyResults, networkContext);
        List<AbstractComputationResult> computationResultList2 = new ArrayList<>();
        computationResultList2.add(result2);
        networkContext.setComputationResultList(computationResultList2);

        //save to DB
        networkContextRepository.save(networkContext);

        Long foundnetworkSecurityAnalysisResult = networkSecurityAnalysisResultRepository.count();
        assertThat(foundnetworkSecurityAnalysisResult)
                .isNotNull();

        List<NetworkSecurityAnalysisResult> networkSecurityAnalysisResult = networkSecurityAnalysisResultRepository.findAll();
        networkSecurityAnalysisResult.forEach(networkSecurityAnalysisResult1 -> {

            assertThat(networkSecurityAnalysisResult1.getPreContingencyResult().getNetworkLimitViolationList())
                    .isNotNull()
                    .isEmpty();
            assertEquals(true, networkSecurityAnalysisResult1.getPreContingencyResult().isComputationOk());
        });
    }

    private NetworkContingency createNetworkContingency() {
        NetworkContingencyElement networkContingencyElement1 = createNetworkContingencyElement();
        NetworkContingencyElement networkContingencyElement2 = createNetworkContingencyElement();
        networkContingencyElement2.setContingencyElementType(ContingencyElementType.GENERATOR);
        networkContingencyElement2.setEquipmentName("equipement2");

        List<NetworkContingencyElement> elements = new ArrayList<>();
        elements.add(networkContingencyElement1);
        elements.add(networkContingencyElement2);
        return new NetworkContingency("contingency", elements);
    }

    private NetworkContingencyElement createNetworkContingencyElement() {
        NetworkContingencyElement networkContingencyElement = new NetworkContingencyElement();
        networkContingencyElement.setContingencyElementType(ContingencyElementType.BRANCH);
        networkContingencyElement.setEquipmentName("equipement1");
        return networkContingencyElement;
    }


    private List<NetworkLimitViolation> createLimitViolations() {
        List<NetworkLimitViolation> limitViolations = new ArrayList<NetworkLimitViolation>();
        NetworkLimitViolation limitViolation1 = new NetworkLimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT.toString(), 500.0d, "CURRENT", 0, 1.0f, 667.67957d);
        NetworkLimitViolation limitViolation2 = new NetworkLimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT.toString(), 500.0d, "CURRENT", 0, 1.0f, 711.42523d);
        limitViolation2.setSide(Branch.Side.TWO.toString());
        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);
        return limitViolations;
    }

    private NetworkContext createNetworkContext() {
        Instant computationDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant networkDate = computationDate.minus(45, ChronoUnit.MINUTES);
        CaseType caseType = new CaseType("srj-ij");
        CaseCategory caseCategory = new CaseCategory("srj");
        caseType = caseTypeRepository.save(caseType);
        caseType.setEnabled(true);
        caseType.setCaseCategory(caseCategory);
        return new NetworkContext(caseType, computationDate, networkDate, "6df341b8-0563-44bd-9b3e-63646c8f0be6", computationDate);
    }
}
