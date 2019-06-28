package com.rte_france.apogee.sea.server.services.computation;

import com.rte_france.apogee.sea.server.model.computation.logic.LimitViolationByIdenfifierAndRemedials;
import com.rte_france.apogee.sea.server.model.computation.logic.SnapshotResult;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.model.dao.user.UsertypeRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.user.Usertype;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.jdbc.AutoConfigureDataJdbc;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ComputationServiceTest.class})
@Transactional
@AutoConfigureDataJpa
@AutoConfigureDataJdbc
@EntityScan("com.rte_france.apogee.sea.server")
@ComponentScan("com.rte_france.apogee.sea.server")
@EnableJpaRepositories("com.rte_france.apogee.sea.server")
@TestPropertySource(locations = "classpath:test.config.properties")
public class ComputationServiceTest {

    @Autowired
    private ComputationService computationService;

    @Autowired
    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @Autowired
    private UsertypeRepository usertypeRepository;

    @Autowired
    NetworkZoneRepository networkZoneRepository;


    @Test
    @Sql({"/test-context-data-service.sql"})
    public void test() {
        String instantExpected = "2018-10-16T07:00:00Z";
        Clock clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.systemDefault());
        Instant instant = Instant.now(clock);

        new MockUp<Instant>() {
            @Mock
            public Instant now() {
                return Instant.now(clock);
            }
        };

        Instant now = Instant.now();

        assertThat(now.toString()).isEqualTo(instantExpected);

        uiSnapshotDaoImpl.insertDataSetInUiSnapshot();

        computationService.fetchLastNetworkContextsWithPriority();
        try {
            LimitViolationByIdenfifierAndRemedials contingencyId = computationService.getLimitViolationsByContingency(null, "contingency0002_id", "1", Collections.singletonList("1"), false);

            SnapshotResult snapshotResult = computationService.getMapNetworkContextByContingency("admin", 1, 10, "1", Arrays.asList("Toulouse_Postes_cc_Zone_Sud", "Lyon_PO_Savoie"), "", false);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(4)
                    .doesNotHaveDuplicates();

            LimitViolationByIdenfifierAndRemedials limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(null, "contingency0003_id", "1", Collections.singletonList("1"), false);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(0)
                    .doesNotHaveDuplicates();

            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(null, "contingency0004_id", "1", Collections.singletonList("61"), false);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(1)
                    .doesNotHaveDuplicates();

            List<String> contextsIds = new ArrayList<>();
            contextsIds.add("60");
            contextsIds.add("9");
            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(null, "contingency0002_id", "1", contextsIds, false);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(2)
                    .doesNotHaveDuplicates();

            snapshotResult = computationService.getMapNetworkContextByContingency("admin", 1, 10, "", Arrays.asList("Lyon_PO_Savoie"), "", false);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(0)
                    .doesNotHaveDuplicates();

            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(null, "contingency0002_id", "1", contextsIds, false);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(2)
                    .doesNotHaveDuplicates();


            // Without exclusion zone filtering
            contextsIds = new ArrayList<>();
            contextsIds.add("7");
            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(null, "AVALLL61VNOL", "1", contextsIds, false);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(4)
                    .doesNotHaveDuplicates();

            // Filtering of only a few constraints, the contingency remains
            Optional<Usertype> actualUserTypeOptional = usertypeRepository.findByName("testExclude");
            Optional<NetworkZone> excludeRegremement1 = networkZoneRepository.findByObjectid("regroupement_1");
            actualUserTypeOptional.get().setExcludeZone(excludeRegremement1.get());
            Usertype actualUserType = usertypeRepository.save(actualUserTypeOptional.get());

            snapshotResult = computationService.getMapNetworkContextByContingency("test", 1, 10, "", null, "", true);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(1)
                    .doesNotHaveDuplicates();

            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(actualUserType, "AVALLL61VNOL", "1", contextsIds, true);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(3)
                    .doesNotHaveDuplicates();

            // Filtering of only a few constraints, the contingency remains
            excludeRegremement1 = networkZoneRepository.findByObjectid("regroupement_2");
            actualUserTypeOptional.get().setExcludeZone(excludeRegremement1.get());
            actualUserType = usertypeRepository.save(actualUserTypeOptional.get());

            snapshotResult = computationService.getMapNetworkContextByContingency("test", 1, 10, "1", null, "", true);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(1)
                    .doesNotHaveDuplicates();

            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(actualUserType, "AVALLL61VNOL", "1", contextsIds, true);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(1)
                    .doesNotHaveDuplicates();

            // Filtering all the constraints, the contingency is filtered
            excludeRegremement1 = networkZoneRepository.findByObjectid("regroupement_3");
            actualUserTypeOptional.get().setExcludeZone(excludeRegremement1.get());
            actualUserType = usertypeRepository.save(actualUserTypeOptional.get());

            snapshotResult = computationService.getMapNetworkContextByContingency("test", 1, 10, "1", null, "", true);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(0)
                    .doesNotHaveDuplicates();

            limitViolationByIdenfifierAndRemedials = computationService.getLimitViolationsByContingency(actualUserType, "AVALLL61VNOL", "1", contextsIds, true);
            assertThat(limitViolationByIdenfifierAndRemedials.getViolations())
                    .hasSize(0)
                    .doesNotHaveDuplicates();

            // The contingency is filtered
            excludeRegremement1 = networkZoneRepository.findByObjectid("regroupement_4");
            actualUserTypeOptional.get().setExcludeZone(excludeRegremement1.get());
            actualUserType = usertypeRepository.save(actualUserTypeOptional.get());

            snapshotResult = computationService.getMapNetworkContextByContingency("test", 1, 10, "1", null, "", true);
            assertThat(snapshotResult.getContingencies())
                    .hasSize(0)
                    .doesNotHaveDuplicates();


        } catch (IComputationService.ComputationServiceException e) {
            e.printStackTrace();
        }

    }
}
