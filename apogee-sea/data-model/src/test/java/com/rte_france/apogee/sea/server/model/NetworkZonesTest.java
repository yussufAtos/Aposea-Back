package com.rte_france.apogee.sea.server.model;

import com.rte_france.apogee.sea.server.model.dao.zones.NetworkBaseVoltageRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkVoltageLevelRepository;
import com.rte_france.apogee.sea.server.model.dao.zones.NetworkZoneRepository;
import com.rte_france.apogee.sea.server.model.zones.NetworkBaseVoltage;
import com.rte_france.apogee.sea.server.model.zones.NetworkVoltageLevel;
import com.rte_france.apogee.sea.server.model.zones.NetworkZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {NetworkZonesTest.class})
@EnableAutoConfiguration
@Transactional
@ActiveProfiles("test")
public class NetworkZonesTest {

    @Autowired
    private NetworkZoneRepository networkZoneRepository;
    @Autowired
    private NetworkBaseVoltageRepository networkBaseVoltageRepository;
    @Autowired
    private NetworkVoltageLevelRepository networkVoltageLevelRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    public void setUp() {

    }

    @AfterDomainEventPublication
    public void setDown() {

    }

    @Test
    public void testCreateVoltageLevel() {

        final String vlId = "VL1";
        final String vlName = "VL1 name";
        final String vlDuplicateName = "VL1 duplicate name";

        NetworkBaseVoltage bv1 = NetworkBaseVoltage.builder()
                .objectid("400")
                .name("400kV")
                .nominalVoltage(400.0)
                .build();
        networkBaseVoltageRepository.save(bv1);

        NetworkVoltageLevel vl1 = NetworkVoltageLevel.builder()
                .objectid(vlId)
                .name(vlName)
                .baseVoltage(bv1)
                .build();
        networkVoltageLevelRepository.save(vl1);

        assertThat(networkVoltageLevelRepository.count())
                .isNotNull()
                .isEqualTo(1);

        Optional<NetworkVoltageLevel> vl = networkVoltageLevelRepository.findByObjectid(vlId);
        assertThat(vl).isPresent();
        vl.ifPresent(v -> assertThat(v.getObjectid()).isEqualTo(vlId));
        vl.ifPresent(v -> assertThat(v.getName()).isEqualTo(vlName));

        NetworkVoltageLevel vl1Duplicate = new NetworkVoltageLevel(vlId, "other", bv1);
        vl1Duplicate.setName(vlDuplicateName);

        Assertions.assertThrows(PersistenceException.class, () -> {
            networkVoltageLevelRepository.save(vl1Duplicate);
            entityManager.flush();
        });

        entityManager.clear();
        networkVoltageLevelRepository.delete(vl1);
        assertThat(networkVoltageLevelRepository.count())
                .isNotNull()
                .isEqualTo(0);
        //verify the baseVoltage is still there
        assertThat(networkBaseVoltageRepository.count())
                .isNotNull()
                .isEqualTo(1);
    }

    @Test
    public void testCreateVoltageLevelWithoutBaseVoltage() {

        final String vlId = "VL1";
        final String vlName = "VL1 name";

        NetworkVoltageLevel vl1 = NetworkVoltageLevel.builder()
                .objectid(vlId)
                .name(vlName)
                .build();
        networkVoltageLevelRepository.save(vl1);

        assertThat(networkVoltageLevelRepository.count())
                .isNotNull()
                .isEqualTo(1);
        Optional<NetworkVoltageLevel> vl = networkVoltageLevelRepository.findByObjectid(vlId);
        assertThat(vl).isPresent();
        vl.ifPresent(v -> assertThat(v.getObjectid()).isEqualTo(vlId));
        vl.ifPresent(v -> assertThat(v.getName()).isEqualTo(vlName));
        vl.ifPresent(v -> assertThat(v.getBaseVoltage()).isNull());
    }

    @Test
    public void testCreateZone() {

        final String zoneId = "Z1";
        final String zoneName = "Z1 name";
        final String zoneDuplicateName = "Z1 duplicate name";

        NetworkZone z1 = NetworkZone.builder().objectid(zoneId).name(zoneName)
                .networkVoltageLevels(Collections.emptySet())
                .build();
        networkZoneRepository.save(z1);

        Long countZones = networkZoneRepository.count();
        assertThat(countZones)
                .isNotNull()
                .isEqualTo(1);

        Optional<NetworkZone> z = networkZoneRepository.findByObjectid(zoneId);
        assertThat(z).isPresent();
        z.ifPresent(v -> assertThat(v.getObjectid()).isEqualTo(zoneId));
        z.ifPresent(v -> assertThat(v.getName()).isEqualTo(zoneName));

        NetworkZone z1Duplicate = new NetworkZone(zoneId, "", Collections.emptySet());
        z1Duplicate.setName(zoneDuplicateName);

        Assertions.assertThrows(PersistenceException.class, () -> {
            networkZoneRepository.save(z1Duplicate);
            entityManager.flush();
        });
    }


    @Test
    public void testCreateZonesWithVoltageLevels() {

        NetworkBaseVoltage bv1 = NetworkBaseVoltage.builder()
                .objectid("400")
                .name("400kV")
                .nominalVoltage(400.0)
                .build();
        networkBaseVoltageRepository.save(bv1);

        // three voltage levels
        NetworkVoltageLevel vl1 = NetworkVoltageLevel.builder()
                .objectid("vl1")
                .baseVoltage(bv1)
                .build();
        NetworkVoltageLevel vl2 = vl1.toBuilder().objectid("vl2").build();
        NetworkVoltageLevel vl3 = vl1.toBuilder().objectid("vl3").build();

        networkVoltageLevelRepository.save(vl1);
        networkVoltageLevelRepository.save(vl2);
        networkVoltageLevelRepository.save(vl3);

        // two zones:
        // - z1 vontains vl1
        // - z2 contains vl2 and vl3
        NetworkZone z1 = NetworkZone.builder().objectid("z1")
                .networkVoltageLevels(Collections.singleton(vl1))
                .build();
        NetworkZone z2 = NetworkZone.builder().objectid("z2")
                .networkVoltageLevels(new HashSet<NetworkVoltageLevel>() {
                    {
                        add(vl2);
                        add(vl3);
                    }
                })
                .build();
        networkZoneRepository.save(z1);
        networkZoneRepository.save(z2);

        // check z1
        Optional<NetworkZone> z1returned = networkZoneRepository.findByObjectid("z1");
        assertThat(z1returned).isPresent();
        z1returned.ifPresent(z -> assertThat(z.getObjectid()).isEqualTo("z1"));
        z1returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().size()).isEqualTo(1));
        z1returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl1)).isTrue());
        z1returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl2)).isFalse());
        z1returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl3)).isFalse());

        // check z2
        Optional<NetworkZone> z2returned = networkZoneRepository.findByObjectid("z2");
        assertThat(z2returned).isPresent();
        z2returned.ifPresent(z -> assertThat(z.getObjectid()).isEqualTo("z2"));
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().size()).isEqualTo(2));
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl1)).isFalse());
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl2)).isTrue());
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl3)).isTrue());

        // modify z2 to contains only vl3
        assertThat(z2.removeVoltageLevels(Collections.singleton(vl2))).isTrue();  // True: deleted
        assertThat(z2.removeVoltageLevels(Collections.singleton(vl2))).isFalse(); // False: not there
        networkZoneRepository.save(z2);

        // check z2 updated content
        z2returned = networkZoneRepository.findByObjectid("z2");
        assertThat(z2returned).isPresent();
        z2returned.ifPresent(z -> assertThat(z.getObjectid()).isEqualTo("z2"));
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().size()).isEqualTo(1));
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl1)).isFalse());
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl2)).isFalse());
        z2returned.ifPresent(z -> assertThat(z.getNetworkVoltageLevels().contains(vl3)).isTrue());


        assertThat(networkZoneRepository.findByNetworkVoltageLevels_Objectid("vl1").contains(z1)).isTrue();
        assertThat(networkZoneRepository.findByNetworkVoltageLevels_Objectid("vl1").contains(z2)).isFalse();

        assertThat(z2.addVoltageLevels(Collections.singleton(vl1))).isTrue();
        assertThat(networkZoneRepository.findByNetworkVoltageLevels_Objectid("vl1").contains(z2)).isTrue();

        networkZoneRepository.delete(z1); // verify it does not cascade delete vl1
        assertThat(networkZoneRepository.findByNetworkVoltageLevels_Objectid("vl1").contains(z1)).isFalse();
        assertThat(networkZoneRepository.findByNetworkVoltageLevels_Objectid("vl1").contains(z2)).isTrue();

        networkZoneRepository.delete(z2);

        //no more zones, but the voltage level remain
        assertThat(networkVoltageLevelRepository.findAll().size()).isEqualTo(3);
        assertThat(networkZoneRepository.findAll().size()).isEqualTo(0);
    }

}
