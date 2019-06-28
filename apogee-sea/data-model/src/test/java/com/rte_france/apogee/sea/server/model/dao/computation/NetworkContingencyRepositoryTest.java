package com.rte_france.apogee.sea.server.model.dao.computation;

import com.powsybl.contingency.ContingencyElementType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingencyElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {NetworkContingencyRepositoryTest.class})
@Transactional
@EnableAutoConfiguration
@EntityScan("com.rte_france.apogee.sea.server.model")
public class NetworkContingencyRepositoryTest {

    @Autowired
    NetworkContingencyRepository networkContingencyRepository;


    @Test
    public void testRepository() {
        NetworkContingency networkContingency = new NetworkContingency("contingency");
        NetworkContingencyElement networkContingencyElement1 = new NetworkContingencyElement("equipement1", ContingencyElementType.BRANCH, networkContingency);
        NetworkContingencyElement networkContingencyElement2 = new NetworkContingencyElement("equipement2", ContingencyElementType.GENERATOR, networkContingency);

        NetworkContingencyElement networkContingencyElement3 = new NetworkContingencyElement("equipement3", ContingencyElementType.BRANCH, networkContingency);
        NetworkContingencyElement networkContingencyElement4 = new NetworkContingencyElement("equipement4", ContingencyElementType.GENERATOR, networkContingency);
        NetworkContingency networkContingency1 = new NetworkContingency("contingency1", networkContingencyElement1, networkContingencyElement2);

        networkContingency.getNetworkContingencyElementList().add(networkContingencyElement1);
        networkContingency.getNetworkContingencyElementList().add(networkContingencyElement2);
        networkContingencyRepository.save(networkContingency);
        Optional<NetworkContingency> foundNetworkContingency = networkContingencyRepository.findById("contingency");
        assertThat(foundNetworkContingency)
                .isNotNull()
                .isNotEmpty();

        assertThat(foundNetworkContingency.get().getNetworkContingencyElementList().size())
                .isNotNull()
                .isEqualTo(2);
    }


}



