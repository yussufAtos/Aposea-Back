package com.rte_france.apogee.sea.server.remedials;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.collect.ImmutableList;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElementType;
import com.powsybl.iidm.network.Branch;
import com.powsybl.security.*;
import com.powsybl.security.json.SecurityAnalysisJsonModule;
import com.powsybl.security.json.SecurityAnalysisResultSerializer;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.ComputationResultRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContextRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.PrioritizeRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.RemedialRepository;
import com.rte_france.apogee.sea.server.model.remedials.Prioritize;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.apogee.sea.server.services.config.AbstractRestServiceConfig;
import com.rte_france.apogee.sea.server.services.prioritize.IPrioritizeRemedialsService;
import com.rte_france.itesla.security.result.LimitViolationBuilder;
import com.rte_france.itesla.security.result.LimitViolations;
import com.rte_france.itesla.variant.json.VariantSimulatorParametersJsonModule;
import com.rte_france.itesla.variant.result.*;
import com.rte_france.powsybl.shortcircuit.converter.ShortCircuitAnalysisJsonModule;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RemedialsServiceTest.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration
@Transactional
@AutoConfigureDataJpa
@ComponentScan(basePackages = "com.rte_france.apogee.sea.server")
@EnableJpaRepositories(basePackages = "com.rte_france.apogee.sea.server")
@EntityScan("com.rte_france.apogee.sea.server")
@TestPropertySource(
        locations = "classpath:apogeetest.remedials.properties")
public class RemedialsServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialsServiceTest.class);

    @Autowired
    @Qualifier("repasProperties")
    AbstractRestServiceConfig config;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private NetworkContextRepository networkContextRepository;

    @Autowired
    private ComputationResultRepository computationResultRepository;

    @Autowired
    private RemedialRepository remedialRepository;

    @Autowired
    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    @Autowired
    private NetworkContingencyRepository networkContingencyRepository;

    @Autowired
    private IPrioritizeRemedialsService iPrioritizeRemedialsService;

    @Autowired
    private PrioritizeRepository prioritizeRepository;

    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    private RemedialsService remedialsService;

    @BeforeEach
    public void setUp() {
        String[] uri = testRestTemplate.getRootUri().split(":");
        LOGGER.info("URI {}", testRestTemplate.getRootUri());
        config.setHostName(uri[1].substring(2));
        config.setPort(Integer.parseInt(uri[2]));
        restTemplate = new RestTemplate();
        remedialsService = new RemedialsService(networkPostContingencyResultRepository, networkContingencyRepository, remedialRepository, config, restTemplate, iPrioritizeRemedialsService);
        mockServer = MockRestServiceServer.createServer(restTemplate);


        NetworkContext networkContext = createNetworkContext();
        NetworkSecurityAnalysisResult computationResult = (NetworkSecurityAnalysisResult) createComputationResult(networkContext, "6df341b8-0563-44bd-9b3e-63646c8f0be6");

        NetworkContingency contingency = createNetworkContingency("LOGELY631", "N-1 Tavel Realtor");
        networkContingencyRepository.save(contingency);
        assertTrue(networkContingencyRepository.findById("N-1 Tavel Realtor").isPresent());
        contingency = networkContingencyRepository.findById("N-1 Tavel Realtor").get();
        NetworkPostContingencyResult networkPostContingencyResult = createPostContingencyResult(contingency, createNetworkLimitViolationsResult());

        NetworkContingency contingency2 = createNetworkContingency("CATG27GROUP.2", "N-1 Argia Cantegrit");
        networkContingencyRepository.save(contingency2);
        assertTrue(networkContingencyRepository.findById("N-1 Argia Cantegrit").isPresent());
        contingency2 = networkContingencyRepository.findById("N-1 Argia Cantegrit").get();
        NetworkPostContingencyResult networkPostContingencyResult2 = createPostContingencyResult(contingency2, createNetworkLimitViolationsResult());

        computationResult.getPostContingencyResults().add(networkPostContingencyResult);
        networkPostContingencyResult.setComputationResult(computationResult);
        computationResult.getPostContingencyResults().add(networkPostContingencyResult2);
        networkPostContingencyResult2.setComputationResult(computationResult);

        List<AbstractComputationResult> list = new ArrayList<>();
        list.add(computationResult);
        networkContext.setComputationResultList(list);

        //save in data base
        networkContextRepository.save(networkContext);

    }

    @AfterEach
    public void setDown() {
        prioritizeRepository.deleteAll();
        remedialRepository.deleteAll();
        networkContextRepository.deleteAll();
        networkContingencyRepository.deleteAll();
    }

    @Test
    public void testAppBaseURI() {
        assertEquals("localhost", config.getHostName());
        assertNotEquals(Boolean.TRUE, config.getSecure());
        assertEquals(testRestTemplate.getRootUri(), config.asUri().toString());
    }

    @Test
    public void testAuthorisation() {
        assertEquals("Basic c29tZVVzZXJuYW1lOnNvbWVQYXNzd29yZA==", config.getBasicAuthentication());
    }

    @Test
    @Transactional
    public void testRemedialsAvailable() {
        Optional<AbstractComputationResult> computationResultOptional = computationResultRepository.findByIdAfsRunner("6df341b8-0563-44bd-9b3e-63646c8f0be6");
        SecurityAnalysisResult securityAnalysisResult = createResult();
        Objects.requireNonNull(securityAnalysisResult);
        StringWriter securityAnalysisResultJson = new StringWriter();
        try {
            SecurityAnalysisResultSerializer.write(securityAnalysisResult, securityAnalysisResultJson);
        } catch (IOException e) {
            LOGGER.error("Error while retrieving the remedials", e);
        }
        mockServer.expect(requestTo(config.asUri().toString() + "/serviceAddress1?updateStatistics=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(securityAnalysisResultJson.toString()))
                .andRespond(withSuccess(readJsonFromFile("logic_remedial_1.json"), MediaType.APPLICATION_JSON));
        String remedials = null;
        try {
            remedialsService.retrieveAndSaveRemedials(createResult(), computationResultOptional.get().getId());
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }
    }


    @Test
    @Transactional
    public void retrieveAndSaveRemedialsTest() throws IRemedialsService.RemedialServiceException {
        Optional<AbstractComputationResult> computationResultOptional = computationResultRepository.findByIdAfsRunner("6df341b8-0563-44bd-9b3e-63646c8f0be6");
        VariantSimulatorResult variantSimulatorResult = createVariantSimulatorResult();
        String json = null;
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper().registerModule(new VariantSimulatorParametersJsonModule())
                    .registerModule(new SecurityAnalysisJsonModule())
                    .registerModule(new ShortCircuitAnalysisJsonModule());
            ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
            StringWriter writer = new StringWriter();
            objectWriter.writeValue(writer, variantSimulatorResult);
            json = writer.toString();
        } catch (IOException e) {
            throw new IRemedialsService.RemedialServiceException("ERROR_RETRIEVING_REMEDIALS", e);
        }
        mockServer.expect(requestTo(config.asUri().toString() + "/serviceAddress1?updateStatistics=true"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(json))
                .andRespond(withSuccess(readJsonFromFile("logic_remedial_1.json"), MediaType.APPLICATION_JSON));
        String remedials = null;
        try {
            remedialsService.retrieveAndSaveRemedials(variantSimulatorResult, computationResultOptional.get().getId());
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Transactional
    public void saveRemedials() {
        Optional<AbstractComputationResult> computationResultOptional = computationResultRepository.findByIdAfsRunner("6df341b8-0563-44bd-9b3e-63646c8f0be6");
        assertTrue(computationResultOptional.isPresent());
        computationResultOptional.ifPresent(computationResult1 -> {
            LOGGER.warn(" Value compt : " + computationResult1.getIdAfsRunner());
            try {
                remedialsService.saveRemedials(readJsonFromFile("logic_remedial_1.json"), computationResult1.getId());
            } catch (IRemedialsService.RemedialServiceException e) {
                LOGGER.error("An exception occurred while retrieving the remedials", e);
            }
            List<Remedial> remedials = remedialRepository.findAll();
            assertThat(remedials)
                    .hasSize(2)
                    .doesNotHaveDuplicates();

            long countnetworkPostContingencyResultTavel = networkPostContingencyResultRepository.findAll().stream().filter(npcr -> npcr.getNetworkContingency().getId().equals("N-1 Tavel Realtor")).count();
            assertEquals(1, countnetworkPostContingencyResultTavel);

            networkPostContingencyResultRepository.findAll().stream().filter(npcr -> npcr.getNetworkContingency().getId().equals("N-1 Tavel Realtor")).forEach(networkPostContingencyResult1 -> {
                assertThat(networkPostContingencyResult1.getRemedials())
                        .isNotEmpty()
                        .hasSize(2);
            });

            networkPostContingencyResultRepository.findAll().stream().filter(npcr -> npcr.getNetworkContingency().getId().equals("N-1 Argia Cantegrit")).forEach(networkPostContingencyResult1 -> {
                assertThat(networkPostContingencyResult1.getRemedials())
                        .isEmpty();
            });
        });
    }


    @Test
    @Transactional
    public void retrieveIalCodeRemedialTest() throws IPrioritizeRemedialsService.PrioritizeRemedialServiceException, JsonParseException, JsonMappingException, IOException {
        NetworkContingency contingency = createNetworkContingency("LOGELY631", "N-1 Tavel Realtor");
        networkContingencyRepository.save(contingency);
        assertTrue(networkContingencyRepository.findById("N-1 Tavel Realtor").isPresent());

        Remedial remedial1 = new Remedial("idRemedialRepository3", "remedial3");
        Remedial remedial2 = new Remedial("idRemedialRepository1", "remedial1");
        remedialRepository.save(remedial1);
        remedialRepository.save(remedial2);

        ObjectMapper mapper = new ObjectMapper().registerModules(new JSR310Module());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Prioritize> prioritizes = mapper.readValue(readJsonFromFile("logic_prioritize_remedial.json"), new TypeReference<List<Prioritize>>() {
        });
        iPrioritizeRemedialsService.savePrioritizeRemedial(prioritizes);

        mockServer.expect(requestTo(config.asUri().toString() + "/serviceAddressIalCodeRemedials?withTestOnConstraints=false"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(readJsonFromFile("logic_remedial_2.json")))
                .andRespond(withSuccess(createIalCodeRemedials(), MediaType.APPLICATION_JSON));

        String ialCodeRemedial = null;
        Instant networkDate = Instant.parse("2018-11-12T18:23:18.692Z");
        try {
            ialCodeRemedial = ((RemedialsService) remedialsService).retrieveIalCodeRemedial(networkDate);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }


    }

    public String readJsonFromFile(String name) {
        try {
            return new String(Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("json/" + name).toURI())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createIalCodeRemedials() {
        return "{\n" +
                "  \"additionalProp1\": {},\n" +
                "  \"additionalProp2\": {},\n" +
                "  \"additionalProp3\": {}\n" +
                "}";
    }

    private AbstractComputationResult createComputationResult(NetworkContext networkContext, String idAfsRunner) {
        NetworkLimitViolationsResult preContingencyResult = new NetworkLimitViolationsResult(true, Collections.emptyList());
        List<NetworkPostContingencyResult> postContingencyResults = new ArrayList<>();
        Instant startDate = networkContext.getComputationDate().plus(5, ChronoUnit.MINUTES);
        Instant endDate = startDate.plus(2, ChronoUnit.MINUTES);
        AbstractComputationResult computationResult = new NetworkSecurityAnalysisResult(preContingencyResult, postContingencyResults, networkContext);
        computationResult.setStartDate(startDate);
        computationResult.setEndDate(endDate);
        computationResult.setName("AS_COMMON");
        computationResult.setIdAfsRunner(idAfsRunner);
        computationResult.setExecStatus(ExecStatus.CREATED);
        return computationResult;
    }


    private NetworkContext createNetworkContext() {
        Instant computationDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant networkDate = computationDate.plus(45, ChronoUnit.MINUTES);
        return new NetworkContext(new CaseType("srj-ij"), computationDate, networkDate, "6df341b8-0563-44bd-9b3e-63646c8f0be6", computationDate);
    }

    private NetworkLimitViolationsResult createNetworkLimitViolationsResult() {
        List<NetworkLimitViolation> limitViolations = new ArrayList<>();
        NetworkLimitViolation limitViolation1 = new NetworkLimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT.toString(), 500.0d, "CURRENT", 0, 1.0f, 667.67957d);
        NetworkLimitViolation limitViolation2 = new NetworkLimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT.toString(), 500.0d, "CURRENT", 0, 1.0f, 711.42523d);
        limitViolation2.setSide(Branch.Side.TWO.toString());
        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);
        return new NetworkLimitViolationsResult(true, limitViolations);
    }

    private NetworkPostContingencyResult createPostContingencyResult(NetworkContingency contingency, NetworkLimitViolationsResult limitViolationsResult) {
        return new NetworkPostContingencyResult(contingency, limitViolationsResult);
    }

    private NetworkContingency createNetworkContingency(String elementId, String contingencyId) {
        NetworkContingency networkContingency = new NetworkContingency(contingencyId);
        NetworkContingencyElement networkContingencyElement = new NetworkContingencyElement(elementId, ContingencyElementType.BRANCH, networkContingency);
        List<NetworkContingencyElement> elements = new ArrayList<>();
        elements.add(networkContingencyElement);
        networkContingency.getNetworkContingencyElementList().addAll(elements);
        return networkContingency;
    }

    private SecurityAnalysisResult createResult() {
        LimitViolationsResult preContingencyResult = new LimitViolationsResult(true, ImmutableList.of(new LimitViolation("s1", LimitViolationType.HIGH_VOLTAGE, 400.0, 1f, 440.0)));
        return new SecurityAnalysisResult(preContingencyResult, Collections.emptyList());
    }

    private VariantSimulatorResult createVariantSimulatorResult() {
        List<LimitViolation> violations = createViolations();
        NetworkMetadata metaData = new NetworkMetadata("id", "sourceFormat", DateTime.now(), 1);
//        NetworkMetadata metaData = new NetworkMetadata(EurostagTutorialExample1Factory.create());
        Contingency contingency1 = new Contingency("HV line 1", singletonList(new BranchContingency("NHV1_NHV2_1")));

        // Create N state results, with 2 variants
        StateResult n = StateResult.builder()
                .initialVariant(VariantResult.builder()
                        .computationOk(true)
                        .violations(violations)
                        .build())
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-11")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-22")
                        .variantResult(VariantResult.failed())
                        .build()
                )
                .build();

        // Create post-contingency result, with 2 variants
        StateResult cont = StateResult.builder()
                .initialVariant(VariantResult.builder()
                        .computationOk(true)
                        .violations(violations)
                        .build())
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-33")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-44")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(emptyList())
                                .build())
                        .build()
                )
                .build();

        PostContingencyVariantsResult contingencyResult = new PostContingencyVariantsResult(contingency1, cont);
        return VariantSimulatorResult.builder()
                .networkMetadata(metaData)
                .preContingencyResult(n)
                .postContingencyResult(contingencyResult)
                .build();
    }

    /**
     * Creates a list of 2 limit violations
     */
    private static List<LimitViolation> createViolations() {
        LimitViolationBuilder violationBuilder = LimitViolations.current()
                .subject("NHV1_NHV2_2")
                .name("CURRENT")
                .duration(0)
                .limit(500)
                .sideOne();
        return ImmutableList.of(violationBuilder.value(667.67957f).sideOne().build(),
                violationBuilder.value(711.42523f).sideTwo().build());
    }

}





