package com.rte_france.apogee.sea.server.model.dao.uisnapshot;

import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContext;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingency;
import com.rte_france.apogee.sea.server.model.uisnapshot.UiSnapshotContingencyContext;
import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.jdbc.AutoConfigureDataJdbc;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {UiSnapshotDaoImplTest.class})
@Transactional
@AutoConfigureDataJpa
@AutoConfigureDataJdbc
@EntityScan("com.rte_france.apogee.sea.server")
@ComponentScan("com.rte_france.apogee.sea.server")
@EnableJpaRepositories("com.rte_france.apogee.sea.server")
@ActiveProfiles("test")
public class UiSnapshotDaoImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(UiSnapshotDaoImpl.class);

    @Autowired
    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @Autowired
    private UiSnapshotContextRepository uiSnapshotContextRepository;

    @Autowired
    private UiSnapshotContingencyRepository uiSnapshotContingencyRepository;

    @Test
    @Sql({"/test-context-data-model.sql"})
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
        List<UiSnapshotContext> uiSnapshotContexts = uiSnapshotContextRepository.findAll();
        assertThat(uiSnapshotContexts)
                .hasSize(31)
                .doesNotHaveDuplicates();

        List<String> zones = Arrays.asList("Toulouse_Postes_cc_Zone_Sud", "Lyon_PO_Savoie");
        Pageable pageable = PageRequest.of(0, 10, Sort.by("networkContingency.id").ascending());
        Page<String> pages = uiSnapshotContingencyRepository.findContingencies(zones, 1L, pageable);
        List<String> contingencies = pages.getContent();
        assertThat(contingencies)
                .hasSize(4)
                .doesNotHaveDuplicates();

        zones = Collections.singletonList("Toulouse_Postes_cc_Zone_Sud");
        pages = uiSnapshotContingencyRepository.findContingencies(zones, 1L, pageable);
        contingencies = pages.getContent();
        assertThat(contingencies)
                .hasSize(3)
                .doesNotHaveDuplicates();

        zones = Collections.singletonList("Lyon_PO_Savoie");
        pages = uiSnapshotContingencyRepository.findContingencies(zones, 1L, pageable);
        contingencies = pages.getContent();
        assertThat(contingencies)
                .hasSize(1)
                .doesNotHaveDuplicates();

        pageable = PageRequest.of(0, 10, Sort.by("networkContingency.id").ascending());
        Page<UiSnapshotContingency> uiSnapshotContingenciesPages = uiSnapshotContingencyRepository.findUiSnapshotContingenciesWithZonesAndSnapshotIdAndPageableAndExcludeZones(zones, 1L, pageable, Collections.singletonList(new NetworkContingency("")));
        List<UiSnapshotContingency> uiSnapshotContingencies = uiSnapshotContingenciesPages.getContent();
        assertThat(uiSnapshotContingencies)
                .hasSize(1)
                .doesNotHaveDuplicates();


        Set<String> remedialsCandidates = new HashSet<>();
        Set<String> remedialsComputed = new HashSet<>();
        Set<String> remedialsEfficient = new HashSet<>();
        for (UiSnapshotContingencyContext uiSnapshotContingencyContext : uiSnapshotContingencies.get(0).getUiSnapshotContingencyContextList()) {
            remedialsCandidates.addAll(uiSnapshotContingencyContext.getRemedialsCandidates());
            remedialsComputed.addAll(uiSnapshotContingencyContext.getRemedialsComputed());
            remedialsEfficient.addAll(uiSnapshotContingencyContext.getRemedialsEfficient());

        }
        assertThat(remedialsEfficient.size()).isEqualTo(1);
        assertThat(remedialsCandidates.size()).isEqualTo(2);
        assertThat(remedialsComputed.size()).isEqualTo(2);

        uiSnapshotContingencies = uiSnapshotContingencyRepository.findUiSnapshotContingencies("contingency0001_id");
        assertThat(uiSnapshotContingencies)
                .hasSize(1)
                .doesNotHaveDuplicates();

        remedialsCandidates = new HashSet<>();
        remedialsComputed = new HashSet<>();
        remedialsEfficient = new HashSet<>();
        for (UiSnapshotContingencyContext uiSnapshotContingencyContext : uiSnapshotContingencies.get(0).getUiSnapshotContingencyContextList()) {
            remedialsCandidates.addAll(uiSnapshotContingencyContext.getRemedialsCandidates());
            remedialsComputed.addAll(uiSnapshotContingencyContext.getRemedialsComputed());
            remedialsEfficient.addAll(uiSnapshotContingencyContext.getRemedialsEfficient());

        }
        assertThat(remedialsEfficient.size()).isEqualTo(2);
        assertThat(remedialsCandidates.size()).isEqualTo(3);
        assertThat(remedialsComputed.size()).isEqualTo(3);


        zones = Arrays.asList("Toulouse_Postes_cc_Zone_Sud", "Lyon_PO_Savoie");
        pageable = PageRequest.of(0, 10, Sort.by("networkContingency.id").ascending());
        uiSnapshotContingenciesPages = uiSnapshotContingencyRepository.findUiSnapshotContingenciesWithZonesAndSnapshotIdAndPageableAndExcludeZones(zones, 1L, pageable, Collections.singletonList(new NetworkContingency("")));
        uiSnapshotContingencies = uiSnapshotContingenciesPages.getContent();
        assertThat(uiSnapshotContingencies)
                .hasSize(4)
                .doesNotHaveDuplicates();
    }

}
