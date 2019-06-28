package com.rte_france.apogee.sea.server.third;

import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.rte_france.apogee.sea.server.afs.AfsProperties;
import com.rte_france.apogee.sea.server.afs.utils.ConvertorDataComputationResult;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.itesla.security.SecurityAnalysisProcessResult;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.security.json.SecurityAnalysisProcessResultJsonConverter;
import com.rte_france.itesla.variant.result.VariantSimulatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class ThirdService implements IThirdService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdService.class);

    private ConvertorDataComputationResult convertorDataComputationResult;

    private NetworkRepository networkRepository;

    private IRemedialsService remedialsService;

    private AfsProperties afsProperties;

    private AppDataBean appDataBean;

    private UiSnapshotDaoImpl uiSnapshotDaoImpl;


    public ThirdService(AfsProperties afsProperties, AppDataBean appDataBean, ConvertorDataComputationResult convertorDataComputationResult,
                        NetworkRepository networkRepository, IRemedialsService remedialsService, UiSnapshotDaoImpl uiSnapshotDaoImpl) {
        this.afsProperties = afsProperties;
        this.appDataBean = appDataBean;
        this.convertorDataComputationResult = convertorDataComputationResult;
        this.networkRepository = networkRepository;
        this.remedialsService = remedialsService;
        this.uiSnapshotDaoImpl = uiSnapshotDaoImpl;
    }

    @Transactional
    public void saveComputationResult(MultipartFile saresult, MultipartFile remedial) throws ThirdServiceException {
        LOGGER.info("MultipartFile saresult OriginalFilename={}, MultipartFile remedial OriginalFilename={}", saresult.getOriginalFilename(), remedial.getOriginalFilename());
        String name = saresult.getOriginalFilename();
        AbstractComputationResult computationResult;
        NetworkContext networkContext;

        try {
            if (name == null || name.isEmpty()) {
                LOGGER.error("security analysis result file name is null or empty");
                return;
            }
            String namesub = name.substring(0, name.length() - 5);
            String[] nameProperties = namesub.split("_");

            LocalDateTime computationDateLocalDateTime = LocalDateTime.parse(nameProperties[2], DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'"));
            ZonedDateTime computationDateZoneDateTime = computationDateLocalDateTime.atZone(ZoneId.of("UTC"));

            LocalDateTime caseDateLocalDateTime = LocalDateTime.parse(nameProperties[1], DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm'Z'"));
            ZonedDateTime caseDateZoneDateTime = caseDateLocalDateTime.atZone(ZoneId.of("UTC"));

            Optional<CaseType> caseTypeOptional = networkRepository.getCaseTypeRepository().findById(nameProperties[0]);
            if (caseTypeOptional.isPresent()) {
                Instant networkDate = caseDateZoneDateTime.toInstant();
                Instant computationDate = computationDateZoneDateTime.toInstant();
                CaseType caseType = caseTypeOptional.get();
                if (networkDate.compareTo(computationDate) < 0) {
                    LOGGER.warn("Apogee-Sea: The processing is stopped: The caseDate is less than computationDate of this imported case (ParentName={}, id ={}) is not enabled.",
                            networkDate,
                            computationDate);
                    return;
                }

                UUID idAfsImportedCase = UUID.randomUUID();

                Instant insertionDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                networkContext = new NetworkContext(caseType, computationDate, networkDate, idAfsImportedCase.toString(), insertionDate);
                networkContext = networkRepository.getNetworkContextRepository().save(networkContext);
                LOGGER.info("Apogee-Sea: Creating a network context type={}, networkDate={}, computationDate={}",
                        caseType.getName(),
                        networkDate,
                        computationDate);

                SecurityAnalysisProcessResult securityAnalysisProcessResult = SecurityAnalysisProcessResultJsonConverter.instance().read(saresult.getInputStream());
                VariantSimulatorResult variantSimulatorResult = securityAnalysisProcessResult.getVariantSimulatorResult();

                computationResult = new NetworkSecurityAnalysisResult();
                computationResult.setNetworkContext(networkContext);
                convertorDataComputationResult.populateNetworkVariantSimulatorResult((NetworkSecurityAnalysisResult) computationResult, variantSimulatorResult);
                networkRepository.getComputationResultRepository().save(computationResult);

                //save remedials
                remedialsService.saveRemedials(new String(remedial.getBytes()), computationResult.getId());
                UUID idAfsRunner = UUID.randomUUID();
                computationResult.setIdAfsRunner(idAfsRunner.toString());
                computationResult.setExecStatus(ExecStatus.COMPLETED);
                computationResult.setName("AS_COMMON");
                Instant startDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                computationResult.setStartDate(startDate);
                computationResult.setNetworkContext(networkContext);
                computationResult.setEndDate(startDate.plusSeconds(60));
                networkRepository.getComputationResultRepository().saveAndFlush(computationResult);
                LOGGER.info("Apogee-Sea: Creating a computation result afsRunnerId={} for network context type={}, networkDate={}, computationDate={}",
                        computationResult.getIdAfsRunner(),
                        networkContext.getCaseType().getName(),
                        networkContext.getNetworkDate(),
                        networkContext.getComputationDate());

                if (networkContext.getCaseType().getCaseCategory().isTriggerUiSnapshot()) {
                    uiSnapshotDaoImpl.handleUiSnapshotCreation();
                }
            }
        } catch (IRemedialsService.RemedialServiceException e) {
            throw new IThirdService.ThirdServiceException("Apogee-Sea: Error while retrieving the remedials", e);
        } catch (IndexOutOfBoundsException e) {
            throw new IThirdService.ThirdServiceException("The name of json is wrong", e);
        } catch (DateTimeParseException e) {
            throw new IThirdService.ThirdServiceException("The values of network date or computation date are wrong", e);
        } catch (IOException e) {
            throw new IThirdService.ThirdServiceException("The json of computation result is wrong", e);
        }
    }


    public void retrieveAndSaveRemedials(String idAfsRunner) throws ThirdServiceException {
        AbstractComputationResult computationResult = null;
        try {
            AppFileSystem afs = appDataBean.getFileSystem(afsProperties.getFileSystemName());
            ProjectFile projectFile = afs.findProjectFile(idAfsRunner, ProjectFile.class);
            SecurityAnalysisResult securityAnalysisResult = null;
            VariantSimulatorResult variantSimulatorResult = null;
            Optional<AbstractComputationResult> abstractComputationResultOptional = networkRepository.getComputationResultRepository().findByIdAfsRunner(idAfsRunner);
            if (abstractComputationResultOptional.isPresent()) {
                computationResult = abstractComputationResultOptional.get();
                NetworkContext networkContext = computationResult.getNetworkContext();
                if (projectFile instanceof SecurityAnalysisProcessRunner) {
                    SecurityAnalysisProcessRunner runnerProcess = (SecurityAnalysisProcessRunner) projectFile;
                    SecurityAnalysisProcessResult securityAnalysisProcessResult = runnerProcess.readResult();
                    variantSimulatorResult = securityAnalysisProcessResult.getVariantSimulatorResult();
                    remedialsService.retrieveAndSaveRemedials(variantSimulatorResult, computationResult.getId());
                }
                if (projectFile instanceof SecurityAnalysisRunner) {
                    SecurityAnalysisRunner runner = (SecurityAnalysisRunner) projectFile;
                    securityAnalysisResult = runner.readResult();
                    remedialsService.retrieveAndSaveRemedials(securityAnalysisResult, computationResult.getId());
                }
                if (networkContext.getCaseType().getCaseCategory().isTriggerUiSnapshot()) {
                    uiSnapshotDaoImpl.handleUiSnapshotCreation();
                }

            }

        } catch (IRemedialsService.RemedialServiceException e) {
            throw new IThirdService.ThirdServiceException("Apogee-Sea: Error while retrieving the remedials for network context type={}, networkDate={}, computationDate={}", e);
        } catch (RuntimeException e) {
            throw new IThirdService.ThirdServiceException("retrieve And SaveRemedials from remedial repository unhandled error", e);
        }
    }
}
