package com.rte_france.apogee.sea.server.tasks;

import com.rte_france.apogee.sea.server.model.computation.CaseCategory;
import com.rte_france.apogee.sea.server.model.computation.CaseType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContext;
import com.rte_france.apogee.sea.server.model.dao.computation.CaseTypeRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContextRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotContingencyContextRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {CleanUpNetworkContextsTaskTest.class})
@Transactional
@AutoConfigureDataJpa
@EntityScan("com.rte_france.apogee.sea.server.model")
@ComponentScan("com.rte_france.apogee.sea.server")
@EnableJpaRepositories("com.rte_france.apogee.sea.server.model")
@ActiveProfiles("test")
public class CleanUpNetworkContextsTaskTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpNetworkContextsTaskTest.class);

    @Autowired
    private NetworkContextRepository networkContextRepository;

    @Autowired
    private CaseTypeRepository caseTypeRepository;

    @Autowired
    private CleanUpAndInsertUiSnapshotTask cleanUpAndInsertUiSnapshotTask;

    @Autowired
    private UiSnapshotContingencyContextRepository uiSnapshotContingencyContextRepository;

    @Autowired
    private CleanUpNetworkContextsTask cleanUpNetworkContextsTask;

    @Test
    public void test1() throws InterruptedException {

        CaseType caseType = new CaseType("srj-ij");
        CaseCategory caseCategory = new CaseCategory("srj");
        caseType.setCaseCategory(caseCategory);
        caseType = caseTypeRepository.save(caseType);
        networkContextRepository.save(createNetworkContext(caseType, 1, 30, 24, 30));
        networkContextRepository.save(createNetworkContext(caseType, 1, 30, 5, 30));
        networkContextRepository.save(createNetworkContext(caseType, 1, 30, 4, 30));
        NetworkContext networkContext = networkContextRepository.save(createNetworkContext(caseType, 24, 30, 8, 30));

        networkContextRepository.save(createNetworkContext(caseType, 2, 30, 9, 30));
        networkContextRepository.save(createNetworkContext(caseType, 2, 30, 7, 30));
        networkContextRepository.save(createNetworkContext(caseType, 2, 30, 6, 30));

        cleanUpAndInsertUiSnapshotTask.run();

        List<NetworkContext> networkContexts = networkContextRepository.findAll();
        assertThat(networkContexts)
                .hasSize(7)
                .doesNotHaveDuplicates()
                .allMatch(n -> "srj-ij".equals(n.getCaseType().getName()));
        cleanUpNetworkContextsTask.run();
        networkContexts = networkContextRepository.findAll();
        assertThat(networkContexts)
                .hasSize(4);
    }


    @Test
    public void test2() throws InterruptedException {
        CaseType caseType = new CaseType("srj-ij");
        CaseCategory caseCategory = new CaseCategory("srj");
        caseType.setCaseCategory(caseCategory);
        caseType = caseTypeRepository.save(caseType);
        networkContextRepository.save(createNetworkContext(caseType, 0, 30, 1, 30));
        networkContextRepository.save(createNetworkContext(caseType, 0, 30, 2, 30));
        networkContextRepository.save(createNetworkContext(caseType, 0, 30, 3, 30));
        networkContextRepository.save(createNetworkContext(caseType, 0, 30, 4, 30));

        List<NetworkContext> networkContexts = networkContextRepository.findAll();
        assertThat(networkContexts)
                .hasSize(4)
                .doesNotHaveDuplicates()
                .allMatch(n -> "srj-ij".equals(n.getCaseType().getName()));
        cleanUpNetworkContextsTask.run();
        networkContexts = networkContextRepository.findAll();
        assertThat(networkContexts)
                .hasSize(2);
    }

    private NetworkContext createNetworkContext(CaseType type, int caseH, int caseM,
                                                int computationH, int computationM) {

        String afsImportedCaseId = computeAfsImportedCaseId(type, caseH, caseM, computationH, computationM);
        String afsRunnerId = computeAfsRunnerId(type, caseH, caseM, computationH, computationM);

        Instant networkDate = hmToInstant(caseH, caseM);
        Instant computationDate = hmToInstant(computationH, computationM);
        NetworkContext networkContext = new NetworkContext(type, computationDate, networkDate,
                afsImportedCaseId, Instant.now(), new ArrayList<>());
        return networkContext;
    }

    private Instant hmToInstant(int h, int m) {
        Instant dateInstant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        dateInstant = dateInstant.minus(m, ChronoUnit.MINUTES);
        dateInstant = dateInstant.minus(h, ChronoUnit.HOURS);
        return dateInstant;
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
