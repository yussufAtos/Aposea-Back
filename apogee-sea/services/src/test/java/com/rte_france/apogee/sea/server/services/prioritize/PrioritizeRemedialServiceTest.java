package com.rte_france.apogee.sea.server.services.prioritize;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.powsybl.contingency.ContingencyElementType;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingencyElement;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.PrioritizeRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.RemedialRepository;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.logic.RemedialIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {PrioritizeRemedialServiceTest.class, PrioritizeRemedialService.class})
@EnableAutoConfiguration
@EnableJpaRepositories(basePackages = "com.rte_france.apogee.sea.server")
@EntityScan("com.rte_france.apogee.sea.server")
@ComponentScan
@ActiveProfiles("test")
public class PrioritizeRemedialServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrioritizeRemedialServiceTest.class);

    @Autowired
    private PrioritizeRepository prioritizeRepository;

    @Autowired
    private NetworkContingencyRepository networkContingencyRepository;

    @Autowired
    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    @Autowired
    private RemedialRepository remedialRepository;


    private IPrioritizeRemedialsService iPrioritizeRemedialsServiceService;

    @BeforeEach
    public void setUp() {
        iPrioritizeRemedialsServiceService = new PrioritizeRemedialService(prioritizeRepository, remedialRepository, networkContingencyRepository, networkPostContingencyResultRepository);
        ((PrioritizeRemedialService) iPrioritizeRemedialsServiceService).setMaxNumPrioritizeRemedial("3");
        prioritizeRepository.deleteAll();
    }

    @Test
    @Transactional
    public void savePrioritizeRemedials() throws IPrioritizeRemedialsService.PrioritizeRemedialServiceException, JsonParseException, JsonMappingException, IOException {
        NetworkContingency contingency = createNetworkContingency("LOGELY631", "N-1 Tavel Realtor");
        networkContingencyRepository.save(contingency);
        assertTrue(networkContingencyRepository.findById("N-1 Tavel Realtor").isPresent());

        Remedial remedial1 = new Remedial("remedial1", "remedial1");
        Remedial remedial2 = new Remedial("remedial2", "remedial2");
        Remedial remedial3 = new Remedial("remedial3", "remedial3");
        remedial1 = remedialRepository.save(remedial1);
        remedial2 = remedialRepository.save(remedial2);
        remedial3 = remedialRepository.save(remedial3);
        assertThat(remedialRepository.findAll())
                .isNotEmpty()
                .hasSize(3);
//        try {
        ObjectMapper mapper = new ObjectMapper().registerModules(new JSR310Module());
        List<Prioritize> prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_1.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findLastPrioritizeRemedialByContingency("N-1 Tavel Realtor"))
                .isNotEmpty()
                .hasSize(1);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(1);

        prioritizes = iPrioritizeRemedialsServiceService.getPrioritizeRemedial("2018-11-12T09:23:18.692Z", "N-1 Tavel Realtor");
        assertThat(prioritizes)
                .isNotEmpty()
                .hasSize(1);

        Map<String, List<RemedialIdentifier>> map = iPrioritizeRemedialsServiceService.findRemedialsPrioritizeByNetworkDate(Instant.parse("2018-11-12T09:23:18.692Z"));
        assertThat(map)
                .isNotEmpty()
                .hasSize(1);


        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_2.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findLastPrioritizeRemedialByContingency("N-1 Tavel Realtor"))
                .isNotEmpty()
                .hasSize(2);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(2);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_3.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(3);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_4.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(5);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_5.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(2);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_6.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(2);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_7.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(2);

        prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial_8.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(3);

        iPrioritizeRemedialsServiceService.deleteByNetworkContingency("N-1 Tavel Realtor");
        assertThat(prioritizeRepository.findAll())
                .isEmpty();
    }

    @Test
    public void saveListEmptyPrioritizeRemedials() throws IPrioritizeRemedialsService.PrioritizeRemedialServiceException, JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper().registerModules(new JSR310Module());
        List<Prioritize> prioritizes = mapper.readValue(readJsonFromFile("list_empty_prioritize_remedial.json"), new TypeReference<List<Prioritize>>() {
        });

        Assertions.assertThrows(IPrioritizeRemedialsService.PrioritizeRemedialServiceException.class, () -> {
            iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        });
    }

    @Test
    public void saveListOfPrioritizeRemedials() throws IPrioritizeRemedialsService.PrioritizeRemedialServiceException, JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper().registerModules(new JSR310Module());
        String json = readJsonFromFile("list_prioritize_remedial.json");
        List<Prioritize> prioritizes = mapper.readValue(json, new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsServiceService.savePrioritizeRemedial(prioritizes);
        assertThat(prioritizeRepository.findAll())
                .isNotEmpty()
                .hasSize(3);
    }


    private NetworkContingency createNetworkContingency(String elementId, String contingencyId) {
        NetworkContingency networkContingency = new NetworkContingency(contingencyId);
        NetworkContingencyElement networkContingencyElement = new NetworkContingencyElement(elementId, ContingencyElementType.BRANCH, networkContingency);
        List<NetworkContingencyElement> elements = new ArrayList<>();
        elements.add(networkContingencyElement);
        networkContingency.getNetworkContingencyElementList().addAll(elements);
        return networkContingency;
    }

    public String readJsonFromFile(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("json/" + name).toURI())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
