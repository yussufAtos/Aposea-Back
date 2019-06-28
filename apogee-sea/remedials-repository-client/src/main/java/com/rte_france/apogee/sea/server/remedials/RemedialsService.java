package com.rte_france.apogee.sea.server.remedials;


import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.*;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.json.SecurityAnalysisJsonModule;
import com.powsybl.security.json.SecurityAnalysisResultSerializer;
import com.rte_france.apogee.sea.server.model.computation.NetworkContingency;
import com.rte_france.apogee.sea.server.model.computation.NetworkPostContingencyResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkContingencyRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkPostContingencyResultRepository;
import com.rte_france.apogee.sea.server.model.dao.remedials.RemedialRepository;
import com.rte_france.apogee.sea.server.model.remedials.Remedial;
import com.rte_france.apogee.sea.server.remedials.logic.MapRemedialByContingency;
import com.rte_france.apogee.sea.server.remedials.logic.RemedialsArray;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.apogee.sea.server.services.config.AbstractRestServiceConfig;
import com.rte_france.apogee.sea.server.services.logic.RemedialIdentifier;
import com.rte_france.apogee.sea.server.services.prioritize.IPrioritizeRemedialsService;
import com.rte_france.apogee.sea.server.services.prioritize.PrioritizeRemedialService;
import com.rte_france.itesla.variant.result.VariantSimulatorResult;
import com.rte_france.powsybl.shortcircuit.converter.ShortCircuitAnalysisJsonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;


@Service
public class RemedialsService implements IRemedialsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemedialsService.class);

    private static final String ERROR_RETRIEVING_REMEDIALS = "Error while retrieving the remedials";

    private static final String UNKNOWN_SERVICE = "Unknown service: ";

    private AbstractRestServiceConfig serviceConfig;

    private NetworkPostContingencyResultRepository networkPostContingencyResultRepository;

    private NetworkContingencyRepository networkContingencyRepository;

    private RemedialRepository remedialRepository;

    private RestTemplate restTemplate = new RestTemplate();

    private IPrioritizeRemedialsService iPrioritizeRemedialsService;

    @Autowired
    RemedialsService(NetworkPostContingencyResultRepository networkPostContingencyResultRepository, RemedialRepository remedialRepository,
                     @Qualifier("repasProperties") AbstractRestServiceConfig serviceConfig, IPrioritizeRemedialsService iPrioritizeRemedialsService,
                     NetworkContingencyRepository networkContingencyRepository) {

        this.networkPostContingencyResultRepository = networkPostContingencyResultRepository;
        this.networkContingencyRepository = networkContingencyRepository;
        this.remedialRepository = remedialRepository;
        this.serviceConfig = serviceConfig;
        this.iPrioritizeRemedialsService = iPrioritizeRemedialsService;
    }

    RemedialsService(NetworkPostContingencyResultRepository networkPostContingencyResultRepository, NetworkContingencyRepository networkContingencyRepository,
                     RemedialRepository remedialRepository, @Qualifier("repasProperties") AbstractRestServiceConfig serviceConfig, RestTemplate restTemplate, IPrioritizeRemedialsService iPrioritizeRemedialsService) {
        this.networkPostContingencyResultRepository = networkPostContingencyResultRepository;
        this.networkContingencyRepository = networkContingencyRepository;
        this.remedialRepository = remedialRepository;
        this.serviceConfig = serviceConfig;
        this.restTemplate = restTemplate;
        this.iPrioritizeRemedialsService = iPrioritizeRemedialsService;

    }

    @Override
    public void retrieveAndSaveRemedials(SecurityAnalysisResult securityAnalysisResult, Long computationResultId) throws RemedialServiceException {
        try {
            String remedials = retrieveRemedials(securityAnalysisResult);
            LOGGER.info("SecurityAnalysisResult : Got remedials from remedials repository for computationResultId={}", computationResultId);
            saveRemedials(remedials, computationResultId);
        } catch (RemedialServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RemedialServiceException(ERROR_RETRIEVING_REMEDIALS, e);
        }
    }

    public String retrieveRemedials(SecurityAnalysisResult securityAnalysisResult) throws RemedialServiceException {
        Objects.requireNonNull(securityAnalysisResult);
        String json = null;
        try {
            StringWriter securityAnalysisResultJson = new StringWriter();
            SecurityAnalysisResultSerializer.write(securityAnalysisResult, securityAnalysisResultJson);
            json = securityAnalysisResultJson.toString();
        } catch (IOException e) {
            throw new RemedialServiceException(ERROR_RETRIEVING_REMEDIALS, e);
        }
        return postRemedials(json);
    }

    @Override
    public void retrieveAndSaveRemedials(VariantSimulatorResult variantSimulatorResult, Long computationResultId) throws RemedialServiceException {
        try {
            String remedials = retrieveRemedials(variantSimulatorResult);
            LOGGER.info("VariantSimulatorResult : got remedials from remedials repository for computationResultId={}", computationResultId);
            saveRemedials(remedials, computationResultId);
        } catch (RemedialServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new RemedialServiceException(ERROR_RETRIEVING_REMEDIALS, e);
        }
    }

    public String retrieveRemedials(VariantSimulatorResult variantSimulatorResult) throws RemedialServiceException {
        Objects.requireNonNull(variantSimulatorResult);
        String json = null;
        try {
            ObjectMapper objectMapper = JsonUtil.createObjectMapper().registerModule(new SecurityAnalysisJsonModule())
                    .registerModule(new SecurityAnalysisJsonModule())
                    .registerModule(new ShortCircuitAnalysisJsonModule());
            ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();
            StringWriter writer = new StringWriter();
            objectWriter.writeValue(writer, variantSimulatorResult);
            json = writer.toString();
        } catch (IOException e) {
            throw new RemedialServiceException(ERROR_RETRIEVING_REMEDIALS, e);
        }
        return postRemedials(json);
    }


    private String postRemedials(String json) {
        String remedials = null;
        // HttpHeaders
        HttpHeaders headers = new HttpHeaders();

        Optional<String> serviceAddress = serviceConfig.getServicePath(RemedialsRepositoryConfig.REMEDIALS_QUERYING);
        String service = serviceAddress.orElseThrow(() -> new NoSuchElementException(UNKNOWN_SERVICE + RemedialsRepositoryConfig.REMEDIALS_QUERYING));

        Optional<String> serviceValueUpdatestatistics = serviceConfig.getServicePath(RemedialsRepositoryConfig.REMEDIALS_QUERYING_UPDATESTATISTICS);
        String valueUpdatestatisticsce = serviceValueUpdatestatistics.orElseThrow(() -> new NoSuchElementException(UNKNOWN_SERVICE + RemedialsRepositoryConfig.REMEDIALS_QUERYING_UPDATESTATISTICS));

        // Authentication
        String authenticationHeader = serviceConfig.getBasicAuthentication();
        headers.set("Authorization", authenticationHeader);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        // Request to return JSON format
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        // Data attached to the request.
        HttpEntity<Stream> requestBody = new HttpEntity(json, headers);
        //Get a new Rest-Template
        String baseRemedialsUrl = serviceConfig.asUri().toString() + "/" + service;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseRemedialsUrl)
                .queryParam("updateStatistics", valueUpdatestatisticsce);

        String remedialsUrl = builder.toUriString();
        LOGGER.info("Remedials repository found at {}", remedialsUrl);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(Charset.forName(StandardCharsets.UTF_8.toString())));

        remedials = restTemplate.postForObject(builder.toUriString(), requestBody, String.class);


        return remedials;
    }

    @Override
    public void saveRemedials(String jsonString, Long computationResultId) throws RemedialServiceException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            // Convert JSON string to Object
            Map<String, RemedialsArray> mapRemedialByContingency = mapper.readValue(jsonString, MapRemedialByContingency.class);
            Map<NetworkPostContingencyResult, RemedialsArray> remedialsByPostContingencyResult = new HashMap<>();
            List<NetworkPostContingencyResult> postContingencyResuls = networkPostContingencyResultRepository.findByComputationResult(computationResultId);
            postContingencyResuls.forEach(networkPostContingencyResult -> remedialsByPostContingencyResult.put(networkPostContingencyResult, mapRemedialByContingency.get(networkPostContingencyResult.getNetworkContingency().getId())));
            List<NetworkPostContingencyResult> postContingencyResultsList = new ArrayList<>();
            List<NetworkContingency> contingencyList = new ArrayList<>();

            if (!mapRemedialByContingency.isEmpty()) {
                LOGGER.warn("Remedials in baseCase are ignored");
            }
            if (!remedialsByPostContingencyResult.isEmpty()) {
                LOGGER.info("RemedialsService: Saving remedials for afsRunnerId={}, type={}, networkDate={}, computationDate={}",
                        postContingencyResuls.get(0).getComputationResult().getIdAfsRunner(),
                        postContingencyResuls.get(0).getComputationResult().getNetworkContext().getCaseType().getName(),
                        postContingencyResuls.get(0).getComputationResult().getNetworkContext().getNetworkDate(),
                        postContingencyResuls.get(0).getComputationResult().getNetworkContext().getComputationDate());
            }
            remedialsByPostContingencyResult.forEach((networkPostContingencyResult, remedialIdentifiers) -> {
                NetworkContingency networkContingency = networkPostContingencyResult.getNetworkContingency();
                List<Remedial> remedials = new ArrayList<>();
                if (remedialIdentifiers != null) {
                    remedialIdentifiers.forEach(remedialIdentifier -> {
                        Remedial remedial = null;
                        Optional<Remedial> remedialOptional = remedialRepository.findById(remedialIdentifier.getIdAbstractLogic());
                        if (remedialOptional.isPresent()) {
                            remedial = remedialOptional.get();
                        } else {
                            remedial = new Remedial(remedialIdentifier.getIdAbstractLogic(), remedialIdentifier.getShortDescription());
                            remedial.setIdLogicContext(remedialIdentifier.getIdLogicContext());
                            remedial = remedialRepository.save(remedial);
                        }
                        remedials.add(remedial);
                    });
                }
                networkContingency.getAllRemedials().addAll(remedials);
                networkPostContingencyResult.setRemedials(remedials);

                contingencyList.add(networkContingency);
                postContingencyResultsList.add(networkPostContingencyResult);
            });
            networkPostContingencyResultRepository.saveAll(postContingencyResultsList);
            networkContingencyRepository.saveAll(contingencyList);
            LOGGER.info("RemedialsService: Saved all remedials.");
        } catch (NoSuchElementException e) {
            throw new RemedialServiceException("Failed networkContingency id", e);
        } catch (JsonGenerationException | JsonMappingException e) {
            throw new RemedialServiceException("Failed mapping remedials json", e);
        } catch (IOException e) {
            throw new RemedialServiceException("Failed generating remedials json", e);
        }
    }

    public String retrieveIalCodeRemedial(Instant networkDate) throws RemedialServiceException {
        String ialCodeRemedials = null;
        // HttpHeaders
        HttpHeaders headers = new HttpHeaders();

        Optional<String> serviceAddress = serviceConfig.getServicePath(RemedialsRepositoryConfig.IAL_CODE_REMEDIALS);
        String service = serviceAddress.orElseThrow(() -> new NoSuchElementException(UNKNOWN_SERVICE + RemedialsRepositoryConfig.IAL_CODE_REMEDIALS));

        Optional<String> serviceValueOfwithtestonconstraints = serviceConfig.getServicePath(RemedialsRepositoryConfig.IAL_CODE_REMEDIALS_WITHTESTONCONSTRAINTS);
        String valueOfWithTestOnConstraints = serviceValueOfwithtestonconstraints.orElseThrow(() -> new NoSuchElementException(UNKNOWN_SERVICE + RemedialsRepositoryConfig.IAL_CODE_REMEDIALS_WITHTESTONCONSTRAINTS));

        try {
            // Authentication
            String authenticationHeader = serviceConfig.getBasicAuthentication();
            headers.set("Authorization", authenticationHeader);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            // Request to return JSON format
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode resultObj = null;
            Map<String, List<RemedialIdentifier>> remedialPrioritize = ((PrioritizeRemedialService) iPrioritizeRemedialsService).findRemedialsPrioritizeByNetworkDate(networkDate);
            String json = mapper.writeValueAsString(remedialPrioritize);
            resultObj = mapper.readTree(json);
            // Data attached to the request.
            HttpEntity<JsonNode> requestBody = new HttpEntity<>(resultObj, headers);
            String remedialsUrl = serviceConfig.asUri().toString() + "/" + service;
            LOGGER.info("Service IAL code remedials found at {}", remedialsUrl);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(remedialsUrl)
                    .queryParam("withTestOnConstraints", valueOfWithTestOnConstraints);

            ialCodeRemedials = restTemplate.postForObject(builder.toUriString(), requestBody, String.class);
            LOGGER.info("ial prioritize remedials from repository ={}", ialCodeRemedials);

        } catch (IOException e) {
            throw new RemedialServiceException("Error while retrieving the ial code remedials", e);
        } catch (Exception e) {
            throw new RemedialServiceException("Error while retrieving the ial code remedial", e);
        }
        return ialCodeRemedials;
    }
}
