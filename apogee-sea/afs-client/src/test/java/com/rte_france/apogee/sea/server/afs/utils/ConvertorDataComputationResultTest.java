package com.rte_france.apogee.sea.server.afs.utils;

import com.powsybl.contingency.BusbarSectionContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Branch;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.LimitViolationsResult;
import com.powsybl.security.PostContingencyResult;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContextRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConvertorDataComputationResultTest.class})
@Transactional
@AutoConfigureDataJpa
@EnableJpaRepositories(basePackages = "com.rte_france.apogee.sea.server.model")
@EntityScan("com.rte_france.apogee.sea.server.model")
@ComponentScan("com.rte_france.apogee.sea.server")
@TestPropertySource(locations = "classpath:apogeetest.properties")
public class ConvertorDataComputationResultTest {

    @Autowired
    ConvertorDataComputationResult convertorDataComputationResult;

    @Autowired
    private NetworkContingencyRepository networkContingencyRepository;

    @Autowired
    NetworkPostContingencyResultRepository NetworkPostContingencyResultRepository;

    @Autowired
    NetworkContextRepository networkContextRepository;

    @BeforeEach
    public void setUp() {

        NetworkContingency networkContingency1 = createNetworkContingency("HV line 1");
        NetworkContingency networkContingency2 = createNetworkContingency("HV line 2");
        NetworkContingency networkContingency3 = createNetworkContingency("HV line 3");
        NetworkContingency networkContingency4 = createNetworkContingency("HV line 4");
        networkContingencyRepository.save(networkContingency1);
        networkContingencyRepository.save(networkContingency2);
        networkContingencyRepository.save(networkContingency3);
        networkContingencyRepository.save(networkContingency4);
    }

    @Test
    public void createNetworkLimitViolationsResultTest() {
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, createLimitViolationsResult(), true);
        assertEquals(2, networkLimitViolationsResult.getNetworkLimitViolationList().size());
        for (NetworkLimitViolation nlv : networkLimitViolationsResult.getNetworkLimitViolationList()) {
            assertEquals("NHV1_NHV2_2", nlv.getSubjectId());
        }
    }

    @Test
    public void createNetworkLimitViolationsResultUsingFilterNonTest() {
        ReflectionTestUtils.setField(convertorDataComputationResult, "voltageConstraintsFilter", VoltageConstraintsFilter.NONE);

        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, createLimitViolationsResultWithMultiTypes(), true);
        assertEquals(1, networkLimitViolationsResult.getNetworkLimitViolationList().size());
    }

    @Test
    public void createNetworkLimitViolationsResultUsingFilterALLTest() {
        ReflectionTestUtils.setField(convertorDataComputationResult, "voltageConstraintsFilter", VoltageConstraintsFilter.ALL);

        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, createLimitViolationsResultWithMultiTypes(), true);
        assertEquals(3, networkLimitViolationsResult.getNetworkLimitViolationList().size());
    }

    @Test
    public void createNetworkLimitViolationsResultUsingFilterBaseCaseTest() {
        ReflectionTestUtils.setField(convertorDataComputationResult, "voltageConstraintsFilter", VoltageConstraintsFilter.BASECASE_ONLY);
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        networkLimitViolationsResult.setNetworkPostContingencyResult(new NetworkPostContingencyResult());
        convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, createLimitViolationsResultWithMultiTypes(), false);
        assertEquals(1, networkLimitViolationsResult.getNetworkLimitViolationList().size());
    }

    @Test
    public void withoutLimitViolationsResultEmpty() {
        PostContingencyResult postContingencyResult1 = createPostContingencyResult("HV line 1");
        PostContingencyResult postContingencyResult2 = createPostContingencyResult("HV line 2");
        PostContingencyResult postContingencyResult3 = new PostContingencyResult(createContingency("HV line 3"), createLimitViolationsResultWithLimitViolationEmpty());
        PostContingencyResult postContingencyResult4 = new PostContingencyResult(createContingency("HV line 4"), createLimitViolationsResultWithComputationOkFalse());
        PostContingencyResult postContingencyResult5 = new PostContingencyResult(createContingency("HV line 5"), createLimitViolationsResultWithComputationOkFalseAndLimitViolationEmpty());
        PostContingencyResult postContingencyResult6 = new PostContingencyResult(createContingency("HV line 6"), createLimitViolationsResultWithComputationOkTrueAndLimitViolationFilterVoltage());

        List<PostContingencyResult> postContingencyResults = new ArrayList<PostContingencyResult>();
        postContingencyResults.add(postContingencyResult1);
        postContingencyResults.add(postContingencyResult2);
        postContingencyResults.add(postContingencyResult3);
        postContingencyResults.add(postContingencyResult4);
        postContingencyResults.add(postContingencyResult5);
        postContingencyResults.add(postContingencyResult6);

        Instant computationDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant networkDate = computationDate.plus(30, ChronoUnit.MINUTES);
        List<AbstractComputationResult> computationResultList = new ArrayList<>();
        NetworkContext networkContext = new NetworkContext(new CaseType("srj-ij"), computationDate, networkDate, "6df341b8-0563-44bd-9b3e-63646c8f0be6", computationDate, computationResultList);
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, createLimitViolationsResult(), true);

        NetworkSecurityAnalysisResult computationResult = new NetworkSecurityAnalysisResult();
        computationResult.setNetworkContext(networkContext);
        computationResult.setPreContingencyResult(networkLimitViolationsResult);
        convertorDataComputationResult.populateNetworkPostContingencyResultList(computationResult, postContingencyResults);
        networkContext.getComputationResultList().add(computationResult);
        networkContextRepository.save(networkContext);

        assertEquals(4, ((NetworkSecurityAnalysisResult) computationResult).getPostContingencyResults().size());
        List<NetworkPostContingencyResult> networkPostContingencyResultsInDatabase = NetworkPostContingencyResultRepository.findAll();
        assertThat(networkPostContingencyResultsInDatabase)
                .isNotNull()
                .size().isEqualTo(4);
    }

    @Test
    public void withNetworkContingencyInBaseTest() {
        PostContingencyResult postContingencyResult = createPostContingencyResult("HV line 1");
        PostContingencyResult postContingencyResult1 = createPostContingencyResult("HV line 2");
        List<PostContingencyResult> postContingencyResults = new ArrayList<PostContingencyResult>();
        postContingencyResults.add(postContingencyResult);
        postContingencyResults.add(postContingencyResult1);

        Instant computationDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant networkDate = computationDate.plus(30, ChronoUnit.MINUTES);
        NetworkContext networkContext = new NetworkContext(new CaseType("srj-ij"), computationDate, networkDate, "6df341b8-0563-44bd-9b3e-63646c8f0be6", computationDate);
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
        NetworkSecurityAnalysisResult computationResult = new NetworkSecurityAnalysisResult();
        computationResult.setNetworkContext(networkContext);
        convertorDataComputationResult.populateNetworkPostContingencyResultList(computationResult, postContingencyResults);
        assertEquals(2, ((NetworkSecurityAnalysisResult) computationResult).getPostContingencyResults().size());
    }


    private AbstractComputationResult createComputationResultWithoutPostContingencyResult() {
        Instant computationDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant networkDate = computationDate.plus(30, ChronoUnit.MINUTES);
        NetworkContext networkContext = new NetworkContext(new CaseType("srj-ij"), computationDate, networkDate, "6df341b8-0563-44bd-9b3e-63646c8f0be6", computationDate);
        NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());

        return new NetworkSecurityAnalysisResult(networkLimitViolationsResult, networkContext);
    }


    private LimitViolationsResult createLimitViolationsResult() {
        LimitViolation limitViolation1 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 667.67957f, Branch.Side.ONE);
        LimitViolation limitViolation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 711.42523f, Branch.Side.TWO);

        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);
        List<String> actionsTaken = new ArrayList<String>();
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");

        return new LimitViolationsResult(true, limitViolations, actionsTaken);
    }


    private LimitViolationsResult createLimitViolationsResultWithLimitViolationEmpty() {
        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        return new LimitViolationsResult(true, limitViolations);
    }

    private LimitViolationsResult createLimitViolationsResultWithComputationOkFalse() {
        LimitViolation limitViolation1 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 667.67957f, Branch.Side.ONE);
        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        limitViolations.add(limitViolation1);
        return new LimitViolationsResult(false, limitViolations);
    }

    private LimitViolationsResult createLimitViolationsResultWithComputationOkFalseAndLimitViolationEmpty() {
        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        return new LimitViolationsResult(false, limitViolations);
    }

    private LimitViolationsResult createLimitViolationsResultWithComputationOkTrueAndLimitViolationFilterVoltage() {
        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        LimitViolation limitViolation = new LimitViolation("NHV1_NHV2_2", LimitViolationType.HIGH_VOLTAGE, "HIGH_VOLTAGE", 0, 500.0f, 1.0f, 711.42523f, Branch.Side.TWO);
        limitViolations.add(limitViolation);
        return new LimitViolationsResult(true, limitViolations);
    }

    private LimitViolationsResult createLimitViolationsResultWithMultiTypes() {
        LimitViolation limitViolation1 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 667.67957f, Branch.Side.ONE);
        LimitViolation limitViolation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.HIGH_VOLTAGE, "HIGH_VOLTAGE", 0, 500.0f, 1.0f, 711.42523f, Branch.Side.TWO);
        LimitViolation limitViolation3 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.LOW_VOLTAGE, "LOW_VOLTAGE", 0, 500.0f, 1.0f, 325.21896f, Branch.Side.ONE);

        List<LimitViolation> limitViolations = new ArrayList<LimitViolation>();
        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);
        limitViolations.add(limitViolation3);
        List<String> actionsTaken = new ArrayList<String>();
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");
        actionsTaken.add("load_shed_100");

        return new LimitViolationsResult(true, limitViolations, actionsTaken);
    }

    private PostContingencyResult createPostContingencyResult(String id) {
        return new PostContingencyResult(createContingency(id), createLimitViolationsResult());
    }

    private Contingency createContingency(String id) {
        ContingencyElement contingencyElement = new BusbarSectionContingency("NHV1_NHV2_1");
        List<ContingencyElement> elements = new ArrayList<ContingencyElement>();
        elements.add(contingencyElement);
        return new Contingency(id, elements);
    }

    private NetworkContingency createNetworkContingency(String id) {
        NetworkContingency networkContingency = new NetworkContingency(id);
        NetworkContingencyElement contingencyElement = new NetworkContingencyElement("NHV1_NHV2_1", ContingencyElementType.BRANCH, networkContingency);
        networkContingency.getNetworkContingencyElementList().add(contingencyElement);
        return networkContingency;
    }
}
