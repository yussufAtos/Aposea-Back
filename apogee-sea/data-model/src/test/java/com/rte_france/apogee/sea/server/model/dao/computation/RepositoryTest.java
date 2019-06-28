package com.rte_france.apogee.sea.server.model.dao.computation;

import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.SnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RepositoryTest.class})
@Transactional
@EnableAutoConfiguration
@EntityScan("com.rte_france.apogee.sea.server.model")
public class RepositoryTest {

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @Autowired
    NetworkContextRepository networkContextRepository;

    @Autowired
    ComputationResultRepository computationResultRepository;

    @Autowired
    private CaseCategoryRepository caseCategoryRepository;

    @MockBean
    private SnapshotRepository snapshotRepository;

    @Autowired
    ComputationDataRepository computationDataRepository;

    private NetworkRepository networkRepository;


    @Test
    public void testRepository() {
        networkRepository = new NetworkRepository(caseTypeRepository, networkContextRepository, computationResultRepository, caseCategoryRepository, snapshotRepository);
        assertNotNull(networkRepository.getNetworkContextRepository());

        CaseType caseType = new CaseType("srj-ij");
        CaseCategory caseCategory = new CaseCategory("srj");
        caseType.setCaseCategory(caseCategory);
        caseType = caseTypeRepository.save(caseType);
        networkContextRepository.save(createNetworkContext(caseType, 8, 30, 6, 0));
        networkContextRepository.save(createNetworkContext(caseType, 8, 30, 7, 0));
        networkContextRepository.save(createNetworkContext(caseType, 8, 30, 8, 0));

        CaseType caseType1 = new CaseType("srmixte");
        CaseCategory caseCategory1 = new CaseCategory("srmixte");
        caseType1.setCaseCategory(caseCategory1);
        caseType1 = caseTypeRepository.save(caseType1);
        networkContextRepository.save(createNetworkContext(caseType1, 8, 30, 8, 0));
        networkContextRepository.save(createNetworkContext(caseType1, 8, 30, 8, 15));

        CaseType caseType2 = new CaseType("pf");
        CaseCategory caseCategory2 = new CaseCategory("pf");
        caseType2.setCaseCategory(caseCategory2);
        caseType2 = caseTypeRepository.save(caseType2);
        networkContextRepository.save(createNetworkContext(caseType2, 8, 30, 8, 30));

        networkContextRepository.save(createNetworkContext(caseType, 9, 30, 7, 0));
        networkContextRepository.save(createNetworkContext(caseType, 9, 30, 8, 0));
        networkContextRepository.save(createNetworkContext(caseType, 9, 30, 9, 0));

        networkContextRepository.save(createNetworkContext(caseType1, 9, 30, 9, 0));
        networkContextRepository.save(createNetworkContext(caseType1, 9, 30, 9, 15));

        networkContextRepository.save(createNetworkContext(caseType2, 9, 30, 9, 30));


        // NetworkContextRepository.findByIdAfsImportedCase
        // ------------------------------------------------

        String key = computeAfsImportedCaseId(caseType, 9, 30, 8, 0);
        Optional<NetworkContext> networkContextOpt = networkContextRepository.findByIdAfsImportedCase(key);
        assertThat(networkContextOpt).isPresent();
        NetworkContext networkContext = networkContextOpt.orElseGet(NetworkContext::new);
        assertThat(networkContext.getCaseType().getName()).isEqualTo("srj-ij");
        assertThat(networkContext.getNetworkDate()).isEqualTo(hmToInstant(9, 30));
        assertThat(networkContext.getComputationDate()).isEqualTo(hmToInstant(8, 0));
        assertThat(networkContext.getIdAfsImportedCase()).isEqualTo(key);

        networkContextOpt = networkContextRepository.findByIdAfsImportedCase("NOPE");
        assertThat(networkContextOpt).isNotPresent();


        // NetworkContextRepository.findByNetworkContextTypeAndComputationDateAndNetworkDate
        // ---------------------------------------------------------------------------------
        networkContextOpt = networkContextRepository.findByCaseTypeAndComputationDateAndNetworkDate("srmixte",
                hmToInstant(8, 0), hmToInstant(8, 30));
        assertThat(networkContextOpt).isPresent();
        networkContext = networkContextOpt.orElseGet(NetworkContext::new);
        assertThat(networkContext.getCaseType().getName()).isEqualTo("srmixte");
        assertThat(networkContext.getNetworkDate()).isEqualTo(hmToInstant(8, 30));
        assertThat(networkContext.getComputationDate()).isEqualTo(hmToInstant(8, 0));

        networkContextOpt = networkContextRepository.findByCaseTypeAndComputationDateAndNetworkDate("pf",
                hmToInstant(12, 0), hmToInstant(12, 0));
        assertThat(networkContextOpt).isNotPresent();


        // NetworkContextRepository.findByNetworkContextType
        // -------------------------------------------------

        List<NetworkContext> networkContexts = networkContextRepository.findByCaseType("srj-ij");
        assertThat(networkContexts)
                .hasSize(6)
                .doesNotHaveDuplicates()
                .allMatch(n -> "srj-ij".equals(n.getCaseType().getName()));


        // NetworkContextRepository.findLatestByNetworkContextType
        // -------------------------------------------------------

        Predicate<NetworkContext> datePredicate = n -> hmToInstant(8, 30).equals(n.getNetworkDate()) ?
                hmToInstant(8, 0).equals(n.getComputationDate()) :
                hmToInstant(9, 0).equals(n.getComputationDate());

        networkContexts = networkContextRepository.findLatestByCaseType("srj-ij");
        assertThat(networkContexts)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(n -> "srj-ij".equals(n.getCaseType().getName()))
                .allMatch(datePredicate);


        networkContexts = networkContextRepository.findLatestByCaseType(caseType);
        assertThat(networkContexts)
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(n -> "srj-ij".equals(n.getCaseType().getName()))
                .allMatch(datePredicate);

        // NetworkContextRepository.findLatestNetworkContexts
        // --------------------------------------------------

        List<String> validKeys = Arrays.asList(
                computeAfsImportedCaseId(new CaseType("srj-ij"), 8, 30, 8, 0),
                computeAfsImportedCaseId(new CaseType("srmixte"), 8, 30, 8, 15),
                computeAfsImportedCaseId(new CaseType("pf"), 8, 30, 8, 30),
                computeAfsImportedCaseId(new CaseType("srj-ij"), 9, 30, 9, 0),
                computeAfsImportedCaseId(new CaseType("srmixte"), 9, 30, 9, 15),
                computeAfsImportedCaseId(new CaseType("pf"), 9, 30, 9, 30)
        );

        networkContexts = networkContextRepository.findLatestNetworkContexts();
        assertThat(networkContexts)
                .hasSize(6)
                .doesNotHaveDuplicates()
                .allMatch(n -> validKeys.contains(n.getIdAfsImportedCase()));


        // ComputationResultRepository.findByIdAfsRunner
        // ---------------------------------------------

        key = computeAfsRunnerId(caseType1, 9, 30, 9, 0);
        Optional<AbstractComputationResult> computationResultOpt = computationResultRepository.findByIdAfsRunner(key);
        assertThat(computationResultOpt).isPresent();
        AbstractComputationResult computationResult = computationResultOpt.orElseGet(NetworkSecurityAnalysisResult::new);
        assertThat(computationResult.getIdAfsRunner()).isEqualTo(key);
        networkContext = computationResult.getNetworkContext();
        assertThat(networkContext).isNotNull();
        assertThat(networkContext.getCaseType().getName()).isEqualTo("srmixte");
        assertThat(networkContext.getNetworkDate()).isEqualTo(hmToInstant(9, 30));
        assertThat(networkContext.getComputationDate()).isEqualTo(hmToInstant(9, 0));


        // ComputationDataRepository.findByIdAfsSecurityAnalysisRunner
        // -----------------------------------------------------------

        key = "CompData";
        ComputationData computationData = new ComputationData(key);
        computationData.setNetworkContextId(3L);
        computationData.setStatus(ExecStatus.COMPLETED);
        computationDataRepository.save(computationData);

        Optional<ComputationData> computationDataOpt = computationDataRepository.findByIdAfsSecurityAnalysisRunner(key);
        assertThat(computationDataOpt).isPresent();
        computationData = computationDataOpt.orElseGet(ComputationData::new);
        assertThat(computationData.getIdAfsSecurityAnalysisRunner()).isEqualTo(key);
        assertThat(computationData.getNetworkContextId()).isEqualTo(3L);
        assertThat(computationData.getStatus()).isEqualTo(ExecStatus.COMPLETED);
    }


    private NetworkContext createNetworkContext(CaseType type, int caseH, int caseM,
                                                int computationH, int computationM) {

        String afsImportedCaseId = computeAfsImportedCaseId(type, caseH, caseM, computationH, computationM);
        String afsRunnerId = computeAfsRunnerId(type, caseH, caseM, computationH, computationM);

        Instant networkDate = hmToInstant(caseH, caseM);
        Instant computationDate = hmToInstant(computationH, computationM);
        NetworkContext networkContext = new NetworkContext(type, computationDate, networkDate,
                afsImportedCaseId, Instant.now(), new ArrayList<>());
        networkContext.getComputationResultList().add(createComputationResult(networkContext, afsRunnerId));

        return networkContext;
    }


    private Instant hmToInstant(int h, int m) {
        return LocalDateTime.of(LocalDate.now(), LocalTime.of(h, m)).atZone(ZoneOffset.systemDefault()).toInstant();
    }


    private AbstractComputationResult createComputationResult(NetworkContext networkContext, String idAfsRunner) {
        NetworkLimitViolationsResult preContingencyResult = new NetworkLimitViolationsResult(true, Collections.emptyList());
        List<NetworkPostContingencyResult> postContingencyResults = new ArrayList<>();
        Instant startDate = networkContext.getComputationDate().plus(3, ChronoUnit.MINUTES);
        Instant endDate = startDate.plus(1, ChronoUnit.MINUTES);
        AbstractComputationResult computationResult = new NetworkSecurityAnalysisResult(preContingencyResult, postContingencyResults, networkContext);
        computationResult.setStartDate(startDate);
        computationResult.setEndDate(endDate);
        computationResult.setName("AS_COMMON");
        computationResult.setIdAfsRunner(idAfsRunner);
        computationResult.setExecStatus(ExecStatus.COMPLETED);
        return computationResult;
    }


    private String computeAfsImportedCaseId(CaseType type, int caseH, int caseM,
                                            int computationH, int computationM) {
        return computeKey(type, caseH, caseM, computationH, computationM, "idAfsImportedCase_");
    }


    private String computeAfsRunnerId(CaseType type, int caseH, int caseM,
                                      int computationH, int computationM) {
        return computeKey(type, caseH, caseM, computationH, computationM, "idAfsRunner_");
    }


    private String computeKey(CaseType type, int caseH, int caseM,
                              int computationH, int computationM, String prefix) {
        return prefix + type.getName() + "_" + caseH + "h" + caseM + "_" + computationH + "h" + computationM;
    }

}
