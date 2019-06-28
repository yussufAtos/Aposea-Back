package com.rte_france.apogee.sea.server.afs;

import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.afs.Project;
import com.powsybl.afs.ProjectFile;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ProjectNode;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.storage.events.*;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.powsybl.security.json.SecurityAnalysisResultDeserializer;
import com.rte_france.apogee.sea.server.afs.exceptions.ApogeeAfsException;
import com.rte_france.apogee.sea.server.afs.utils.ConvertorDataComputationResult;
import com.rte_france.apogee.sea.server.afs.utils.UtilityAfs;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.ComputationDataRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.itesla.security.SecurityAnalysisProcessResult;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.variant.result.VariantSimulatorResult;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;

@Component
public class ApogeeAfsAppStorageListener implements AppStorageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApogeeAfsAppStorageListener.class);

    @Getter
    @Setter
    private AfsProperties afsProperties;

    @Getter
    @Setter
    private AppDataBean appDataBean;

    private ConvertorDataComputationResult convertorDataComputationResult;

    private UtilityAfs utilityAfs;

    private ComputationDataRepository computationDataRepository;

    private NetworkRepository networkRepository;

    private IRemedialsService remedialsService;

    @Autowired
    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @Autowired
    public ApogeeAfsAppStorageListener(AfsProperties afsProperties, AppDataBean appDataBean, UtilityAfs utilityAfs,
                                       ConvertorDataComputationResult convertorDataComputationResult,
                                       ComputationDataRepository computationDataRepository, NetworkRepository networkRepository, IRemedialsService remedialsService) {
        this.afsProperties = afsProperties;
        this.appDataBean = appDataBean;
        this.utilityAfs = utilityAfs;
        this.convertorDataComputationResult = convertorDataComputationResult;
        this.computationDataRepository = computationDataRepository;
        this.networkRepository = networkRepository;
        this.remedialsService = remedialsService;

    }

    @PreDestroy
    public void destroy() {
        appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName()).removeListener(this);
    }

    /**
     * <p>React on events sent by AFS.</p>
     *
     * @param nodeEventList list of events on AFS
     */
    @Override
    public void onEvents(NodeEventList nodeEventList) {
        for (NodeEvent nodeEvent : nodeEventList.getEvents()) {
            try {
                NodeEventType nodeEventType = nodeEvent.getType();
                switch (nodeEventType) {
                    case NODE_CREATED:
                        loggingEventAfs(nodeEvent, null);
                        break;
                    case NODE_DATA_UPDATED:
                        String dataName = ((NodeDataUpdated) nodeEvent).getDataName();
                        if (loggingEventAfs(nodeEvent, dataName)) {
                            nodeDataUpdatedProcessing(nodeEvent);
                        }
                        break;
                    default:
                        LOGGER.debug("Ignored AFS Event: EventType = {}", nodeEvent.getType());
                        break;
                }
            } catch (RuntimeException e) {
                LOGGER.error("onEvents unhandled error: Type={}, Id={}",
                        nodeEvent.getType(),
                        nodeEvent.getId(),
                        e);
            }
        }
    }

    private boolean loggingEventAfs(NodeEvent nodeEvent, String dataName) {
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        NodeInfo nodeInfo = storage.getNodeInfo(nodeEvent.getId());
        Optional<NodeInfo> parentNodeInfoOptional = storage.getParentNode(nodeEvent.getId());
        NodeInfo parentNodeInfo;
        if (parentNodeInfoOptional.isPresent()) {
            parentNodeInfo = parentNodeInfoOptional.get();
            if (parentNodeInfoOptional.get().getPseudoClass().equals(ProjectFolder.PSEUDO_CLASS)) {
                String baseLogging = "AFS Event: Type={}, NodeName={}, Id={}, PseudoClass={}, ParentNodeName={}";
                if (parentNodeInfo.getName().equals(afsProperties.getProjectFolder())) {
                    LOGGER.info(baseLogging,
                            nodeEvent.getType(),
                            nodeInfo.getName(),
                            nodeEvent.getId(),
                            nodeInfo.getPseudoClass(),
                            parentNodeInfo.getName());
                    return false;
                } else {
                    Optional<NodeInfo> rootNodeInfoOptional = storage.getParentNode(parentNodeInfo.getId());
                    if (rootNodeInfoOptional.isPresent()) {
                        NodeInfo rootNodeInfo = rootNodeInfoOptional.get();
                        return navigateTreeAfs(nodeEvent, storage, nodeInfo, parentNodeInfo, rootNodeInfo, baseLogging, dataName);
                    }

                }
            }
        }
        return false;
    }

    private Boolean navigateTreeAfs(NodeEvent nodeEvent, ListenableAppStorage storage, NodeInfo nodeInfo, NodeInfo parentNodeInfo, NodeInfo rootNodeInfo, String baseLogging, String dataName) {
        String rootNodeName = rootNodeInfo.getName();
        if (rootNodeInfo.getPseudoClass().equals(Project.PSEUDO_CLASS)) {
            if (rootNodeName.equals(afsProperties.getProjectFolder())) {
                LOGGER.info(baseLogging,
                        nodeEvent.getType(),
                        nodeInfo.getName(),
                        nodeEvent.getId(),
                        nodeInfo.getPseudoClass(),
                        parentNodeInfo.getName());
                return false;
            } else {
                LOGGER.warn("AFS Event: Type={}, NodeName={}, id={}, PseudoClass={}, ParentNodeName={} is located outside of root folder {} and therefore will not be processed.",
                        nodeEvent.getType(),
                        nodeInfo.getName(),
                        nodeInfo.getId(),
                        nodeInfo.getPseudoClass(),
                        parentNodeInfo.getName(),
                        afsProperties.getProjectFolder());
                return false;
            }
        } else {
            Optional<NodeInfo> projectNodeInfoOptional = storage.getParentNode(rootNodeInfo.getId());
            String name = dataName != null ? dataName : nodeInfo.getName();
            if (projectNodeInfoOptional.isPresent()) {
                String projectNodeName = projectNodeInfoOptional.get().getName();
                if (projectNodeName.equals(afsProperties.getProjectFolder())) {
                    LOGGER.info("AFS Event: Type={}, dataName={}, Id={}, PseudoClass={}, ParentNodeName={}",
                            nodeEvent.getType(),
                            name,
                            nodeEvent.getId(),
                            nodeInfo.getPseudoClass(),
                            parentNodeInfo.getName());
                    return true;
                } else {
                    LOGGER.warn("AFS Event: Type={}, dataName={}, id={}, PseudoClass={}, ParentNodeName={},  projectName={} is located outside of root folder {} and therefore will not be processed.",
                            nodeEvent.getType(),
                            name,
                            nodeInfo.getId(),
                            nodeInfo.getPseudoClass(),
                            parentNodeInfo.getName(),
                            projectNodeName,
                            afsProperties.getProjectFolder());
                    return false;
                }
            }
        }
        return false;
    }


    /**
     * <p>Processing for node creation events.</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener.
     */
    private boolean nodeCreatedProcessing(NodeEvent nodeEvent) {
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        NodeInfo nodeInfo = storage.getNodeInfo(nodeEvent.getId());
        Optional<NodeInfo> parentNodeInfoOptional = storage.getParentNode(nodeEvent.getId());
        String parentNodeName = null;
        if (parentNodeInfoOptional.isPresent()) {
            parentNodeName = parentNodeInfoOptional.get().getName();
        }

        if (ImportedCase.PSEUDO_CLASS.equals(nodeInfo.getPseudoClass())) {
            return processingAfterImportedCaseCreated(nodeEvent);

        } else {
            LOGGER.debug("AFS Event: Ignored AFS Event: Type={}, NodeName={}, Id={}, ParentNodeName={}, PseudoClass={}",
                    nodeEvent.getType(),
                    nodeInfo.getName(),
                    nodeEvent.getId(),
                    parentNodeName,
                    nodeInfo.getPseudoClass());
            return false;
        }
    }

    /**
     * <p>Processing for node data update events.</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener.
     */
    private void nodeDataUpdatedProcessing(NodeEvent nodeEvent) {
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        String dataName = ((NodeDataUpdated) nodeEvent).getDataName();
        NodeInfo nodeInfo = storage.getNodeInfo(nodeEvent.getId());
        Optional<NodeInfo> parentNodeInfoOptional = storage.getParentNode(nodeEvent.getId());
        String parentNodeName = null;
        if (parentNodeInfoOptional.isPresent()) {
            parentNodeName = parentNodeInfoOptional.get().getName();
        }
        switch (dataName) {
            // After receiving the end of the deposition of the network situation, Create a runner object
            case "parameters":
                if (ImportedCase.PSEUDO_CLASS.equals(nodeInfo.getPseudoClass())) {
                    boolean createdContingenciesOk = nodeCreatedProcessing(nodeEvent);
                    createSecurityAnalysisRunnerAndNetworkContext(nodeEvent, createdContingenciesOk);
                }
                break;
            // Launch a computation
            case "parametersJson":
                if ("securityAnalysisRunner".equals(nodeInfo.getPseudoClass()) || "securityAnalysisProcessRunner".equals(nodeInfo.getPseudoClass())) {
                    launchSecurityAnalysisRunner(nodeEvent);
                }
                break;

            // After receiving the calculation notification iTesla, save the results
            case "resultJsonV2":
                if ("securityAnalysisRunner".equals(nodeInfo.getPseudoClass()) || "securityAnalysisProcessRunner".equals(nodeInfo.getPseudoClass())) {
                    saveSecurityAnalysisRunnerResults(nodeEvent);
                }
                break;
            default:
                LOGGER.debug("AFS Event: Ignored AFS Event Type={}, Id={}, DataName={}, NodeName={}, ParentNodeName={}, PseudoClass={}",
                        nodeEvent.getType(),
                        nodeEvent.getId(),
                        dataName,
                        nodeInfo.getName(),
                        parentNodeName,
                        nodeInfo.getPseudoClass());
                break;
        }
    }

    /**
     * <p>Processing for ImportedCase creation event. At the moment limited to creation of contingency list.</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener.
     */
    private boolean processingAfterImportedCaseCreated(NodeEvent nodeEvent) {
        Optional<String> contingenciesIdOptional = copyContingencyList(nodeEvent);
        ImportedCase importedCase = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(nodeEvent.getId(), ImportedCase.class);
        Optional<ProjectFolder> importedCaseParentOptional = importedCase.getParent();
        String importedCaseParentName = null;
        if (importedCaseParentOptional.isPresent()) {
            importedCaseParentName = importedCaseParentOptional.get().getName();
        }
        Properties properties = importedCase.getParameters();
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf(properties.getProperty("sea.case_date")));

        String ialCodeRemedial = null;
        if (contingenciesIdOptional.isPresent()) {
            try {
                ialCodeRemedial = remedialsService.retrieveIalCodeRemedial(networkDate);
                this.utilityAfs.concatenateIalWithDefault(contingenciesIdOptional.get(), ialCodeRemedial);
                return true;
            } catch (NumberFormatException | DateTimeException e) {
                LOGGER.error("The date format of this imported case (id ={}, ParentName={}) are wrong.",
                        importedCase.getId(),
                        importedCaseParentName, e);
            } catch (IRemedialsService.RemedialServiceException e) {
                LOGGER.error("Error while retrieving the ial code remedials for event : nodeId ={}, parentNodeName ={}",
                        importedCase.getId(),
                        importedCaseParentName, e);
            } catch (ApogeeAfsException e) {
                LOGGER.error(e.getMessage() + " for event : nodeId ={}, parentNodeName ={}",
                        importedCase.getId(),
                        importedCaseParentName, e);
            }
            return false;
        }
        return false;
    }

    private Optional<String> copyContingencyList(NodeEvent nodeEvent) {
        final String importedCaseId = nodeEvent.getId();
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        ImportedCase importedCase = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(importedCaseId, ImportedCase.class);

        Optional<ProjectFolder> caseFolderInfo = importedCase.getParent();

        String[] pathContingencies = afsProperties.getPathContingencies().split("/");
        String fileNameContingencies = null;
        Optional<NodeInfo> contingenciesInfo = Optional.empty();

        if (!caseFolderInfo.isPresent()) {
            LOGGER.warn("AFS Event: A problem occurred when creating the ContingencyList in the case folder: the ImportedCase {} parent does not exist.", importedCaseId);
            return Optional.empty();
        }

        if (pathContingencies.length == 2) {
            fileNameContingencies = pathContingencies[pathContingencies.length - 1];
            contingenciesInfo = storage.getChildNode(caseFolderInfo.get().getId(), fileNameContingencies);
        }

        if (contingenciesInfo.isPresent()) {
            LOGGER.warn("AFS Event: The contingencies file {} already exists in the case folder {}. The ImportedCase will be ignored.",
                    fileNameContingencies, caseFolderInfo.get().getName());
            return Optional.empty();
        }

        ProjectNode caseFolder = caseFolderInfo.get();
        String contingenciesId = null;
        if (utilityAfs.isChildFolderSeaRoot(caseFolder)) {
            try {
                Optional<NodeInfo> configNodeInfo = utilityAfs.getConfigFolderInfo(utilityAfs.getSeaRootFolder(), storage);

                if (configNodeInfo.isPresent()) {
                    contingenciesId = utilityAfs.importContingenciesInCaseFolder(caseFolder, configNodeInfo.get(), storage);
                } else {
                    contingenciesId = utilityAfs.createContingenciesEmptyInCaseFolder(caseFolder).getId();
                }

                return Optional.of(contingenciesId);

            } catch (ApogeeAfsException e) {
                LOGGER.error("AFS Event: Failed copying contingencies for ImportedCase {}:", importedCaseId, e);
            }
        } else {
            LOGGER.warn("AFS Event: ImportedCase id={}, parentNodeName={} is located outside of root folder {} and therefore will not be processed.",
                    importedCaseId,
                    caseFolderInfo.get().getName(),
                    afsProperties.getProjectFolder());
        }
        return Optional.empty();
    }

    /**
     * <p>parameters is the last data written to an ImportedCase, This event indicates that the ImportedCase is fully written to AFS.</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener.
     */
    private void createSecurityAnalysisRunnerAndNetworkContext(NodeEvent nodeEvent, boolean createdContingenciesWithIalOk) {
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        ImportedCase importedCase = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(nodeEvent.getId(), ImportedCase.class);
        Optional<ProjectFolder> caseFolderInfo = importedCase.getParent();
        //create runner object from importedCase and actionScript in caseFolder
        if (caseFolderInfo.isPresent()) {
            ProjectFolder caseFolder = caseFolderInfo.get();
            if (!utilityAfs.isChildFolderSeaRoot(caseFolder)) {
                LOGGER.warn("AFS Event: ImportedCase id={}, parentNodeName={} is located outside of root folder {} and therefore will not be processed.",
                        importedCase.getId(),
                        caseFolderInfo.get().getName(),
                        afsProperties.getProjectFolder());
                return;
            }
            String[] pathContingencies = afsProperties.getPathContingencies().split("/");
            String fileNameContingencies = pathContingencies[pathContingencies.length - 1];
            boolean isCreateContingenciesWithRemedial = afsProperties.isContingenciesWithRemedial() && createdContingenciesWithIalOk;
            if (isCreateContingenciesWithRemedial) {
                fileNameContingencies = "defautsWithIal";
            }
            Optional<NodeInfo> actionScriptInfo = storage.getChildNode(caseFolder.getId(), fileNameContingencies);
            if (actionScriptInfo.isPresent()) {
                Optional<String> runnerOptional = createRunner(importedCase, caseFolder, actionScriptInfo.get());
                String runnerId;
                if (runnerOptional.isPresent()) {
                    runnerId = runnerOptional.get();
                    createNetworkContext(importedCase, runnerId, createdContingenciesWithIalOk);
                }
            } else {
                LOGGER.error("ImportedCase id={}, parentNodeName={} failed to retrieve iAL file {}",
                        importedCase.getId(),
                        caseFolderInfo.get().getName(),
                        fileNameContingencies);
            }
        } else {
            LOGGER.warn("AFS Event: A problem occurred when creating the runner object in the case folder: the imported case's parent does not exist.");
        }
    }

    private Optional<String> createRunner(ImportedCase importedCase, ProjectFolder caseFolder, NodeInfo actionScriptInfo) {
        String actionScriptId = actionScriptInfo.getId();
        ActionScript actionScript = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(actionScriptId, ActionScript.class);
        Optional<String> runnerOptional;

        if (afsProperties.isAsynchronousRunner()) {
            runnerOptional = utilityAfs.buildSecurityAnalysisProcessRunner(caseFolder, importedCase, actionScript);
        } else {
            runnerOptional = utilityAfs.buildSecurityAnalysisRunner(caseFolder, importedCase, actionScript);
        }
        return runnerOptional;
    }

    /**
     * <p>Create a network context with id afs security analysis runner</p>
     *
     * @param importedCase An imported case
     * @param idAfsRunner  A security analysis runner id
     */
    private void createNetworkContext(ImportedCase importedCase, String idAfsRunner, boolean createdContingenciesWithIalOk) {
        Optional<NetworkContext> networkContextInfo = networkRepository.getNetworkContextRepository().findByIdAfsImportedCase(importedCase.getId());
        ComputationData computationData = new ComputationData(idAfsRunner);
        Optional<ProjectFolder> parentNodeOptional = importedCase.getParent();
        Properties propertiesCase = importedCase.getParameters();
        String parentNodeName = null;
        String type = propertiesCase.getProperty("sea.computation_type");

        if (parentNodeOptional.isPresent()) {
            parentNodeName = parentNodeOptional.get().getName();
        }
        if (!networkContextInfo.isPresent()) {
            if (isTypeNotValid(importedCase, computationData, parentNodeName, type)) {
                return;
            }
            saveNetworkContext(importedCase, computationData, propertiesCase, parentNodeName, type, createdContingenciesWithIalOk);
        } else {
            computationData.setStatus(ExecStatus.FAILED);
            computationDataRepository.save(computationData);
            LOGGER.warn("Apogee-Sea: The processing is stopped: a network context with this imported case (id={}, ParentName={}) already exists in the database.",
                    importedCase.getId(),
                    parentNodeName);
        }
    }

    private void saveNetworkContext(ImportedCase importedCase, ComputationData computationData, Properties propertiesCase, String parentNodeName, String type, boolean createdContingenciesWithIalOk) {
        Optional<NetworkContext> networkContextInfo;
        CaseType caseType;
        try {
            Optional<CaseType> caseTypeOptional = networkRepository.getCaseTypeRepository().findById(type);
            if (caseTypeOptional.isPresent()) {
                caseType = caseTypeOptional.get();
                Instant networkDate = Instant.ofEpochMilli(Long.valueOf(propertiesCase.getProperty("sea.case_date")));
                Instant computationDate = Instant.ofEpochMilli(Long.valueOf(propertiesCase.getProperty("sea.computation_date")));
                if (networkDate.compareTo(computationDate) < 0) {
                    LOGGER.warn("Apogee-Sea: The processing is stopped: The caseDate is less than computationDate of this imported case (ParentName={}, id ={}) is not enabled.",
                            parentNodeName,
                            importedCase.getId());
                    return;
                }
                networkContextInfo = networkRepository.getNetworkContextRepository().findByCaseTypeAndComputationDateAndNetworkDate(caseType.getName(), computationDate, networkDate);

                NetworkContext networkContext;
                if (networkContextInfo.isPresent()) {
                    computationData.setStatus(ExecStatus.FAILED);
                    computationDataRepository.save(computationData);
                    LOGGER.warn("Apogee-Sea: The processing is stopped: a network context with (type={}, networkDate={}, computationDate={}) already exists in the database.",
                            caseType.getName(),
                            networkDate,
                            computationDate);
                    return;
                } else {
                    Instant insertionDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                    networkContext = new NetworkContext(caseType, computationDate, networkDate, importedCase.getId(), insertionDate);
                    networkContext = networkRepository.getNetworkContextRepository().save(networkContext);
                    LOGGER.info("Apogee-Sea: Creating a network context type={}, networkDate={}, computationDate={}",
                            caseType.getName(),
                            networkDate,
                            computationDate);
                }
                computationData.setNetworkContextId(networkContext.getId());
                computationData.setComputationWithCodeIal(createdContingenciesWithIalOk);
                computationData.setStatus(ExecStatus.CREATED);
                computationDataRepository.save(computationData);
            }
        } catch (NumberFormatException | DateTimeException e) {
            computationData.setStatus(ExecStatus.FAILED);
            computationDataRepository.save(computationData);
            LOGGER.warn("Apogee-Sea: The processing is stopped: The date format of this imported case (id ={}, ParentName={}) are wrong.",
                    importedCase.getId(),
                    parentNodeName);
        }
    }

    private boolean isTypeNotValid(ImportedCase importedCase, ComputationData computationData, String parentNodeName, String type) {
        if (type == null || type.isEmpty()) {
            computationData.setStatus(ExecStatus.FAILED);
            computationDataRepository.save(computationData);
            LOGGER.warn("Apogee-Sea: Event: The processing is stopped: missing property sea.computation_type of this imported case (ParentName={}, id ={}, name = {}).",
                    parentNodeName,
                    importedCase.getId(),
                    importedCase.getName());
            return true;
        }
        Optional<CaseType> caseTypeOptional = networkRepository.getCaseTypeRepository().findById(type);
        if (!caseTypeOptional.isPresent()) {
            computationData.setStatus(ExecStatus.FAILED);
            computationDataRepository.save(computationData);
            LOGGER.warn("Apogee-Sea: The processing is stopped: The case type of this imported case (ParentName={}, id={}) does not exist in dataBase.",
                    importedCase.getId(),
                    parentNodeName);
            return true;
        }
        CaseType caseType = caseTypeOptional.get();
        if (!caseType.isEnabled()) {
            computationData.setStatus(ExecStatus.FAILED);
            computationDataRepository.save(computationData);
            LOGGER.warn("Apogee-Sea: The processing is stopped: The case type of this imported case (ParentName={}, id ={}) is not enabled.",
                    importedCase.getId(),
                    parentNodeName);
            return true;
        }
        return false;
    }

    /**
     * * <p>Once the runner created, launch the corresponding computation</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener (at the end of the runner creation).
     */
    private void launchSecurityAnalysisRunner(NodeEvent nodeEvent) {
        Optional<ComputationData> computationDataOptional = computationDataRepository.findByIdAfsSecurityAnalysisRunner(nodeEvent.getId());
        ComputationData computationData;
        if (computationDataOptional.isPresent()) {
            computationData = computationDataOptional.get();
            if (computationData.getStatus().equals(ExecStatus.CREATED)) {
                ProjectFile projectFile = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(nodeEvent.getId(), ProjectFile.class);
                Optional<NetworkContext> networkContextOptional = networkRepository.getNetworkContextRepository().findById(computationData.getNetworkContextId());
                NetworkContext networkContext;
                if (networkContextOptional.isPresent()) {
                    networkContext = networkContextOptional.get();
                    NetworkLimitViolationsResult networkLimitViolationsResult = new NetworkLimitViolationsResult(true, new ArrayList<>());
                    AbstractComputationResult computationResult = new NetworkSecurityAnalysisResult(networkLimitViolationsResult, networkContextOptional.get());
                    computationResult.setIdAfsRunner(projectFile.getId());
                    computationResult.setExecStatus(ExecStatus.RUNNING);
                    computationResult.setName("AS_COMMON");
                    computationResult.setComputationWithRemedials(computationData.isComputationWithCodeIal());
                    Instant startDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                    computationResult.setStartDate(startDate);
                    computationResult.setNetworkContext(networkContextOptional.get());
                    networkRepository.getComputationResultRepository().save(computationResult);
                    LOGGER.info("Apogee-Sea: Creating a computation result afsRunnerId={} for network context type={}, networkDate={}, computationDate={}",
                            computationResult.getIdAfsRunner(),
                            networkContext.getCaseType().getName(),
                            networkContext.getNetworkDate(),
                            networkContext.getComputationDate());
                    computationData.setStatus(ExecStatus.RUNNING);
                    computationDataRepository.save(computationData);
                    startComputation(projectFile);
                }
            }
        }
    }

    private void startComputation(ProjectFile projectFile) {
        Optional<ProjectFolder> parentNodeOptional = projectFile.getParent();
        String parentNodeName = null;
        if (parentNodeOptional.isPresent()) {
            parentNodeName = parentNodeOptional.get().getName();
        }
        if (projectFile instanceof SecurityAnalysisProcessRunner) {
            if (afsProperties.isLaunchRunner()) {
                LOGGER.info("AFS Event: Start a computation on runnerName={}, Id={}, ParentNode={}",
                        projectFile.getName(),
                        projectFile.getId(),
                        parentNodeName);
                ((SecurityAnalysisProcessRunner) projectFile).start();
            } else {
                saveSecurityAnalysisRunnerResultsWithoutItesla(projectFile.getId());
                LOGGER.info("Apogee-Sea: The computation didn't start because the launchRunner value of runnerName={}, Id={}, ParentNode={} is false",
                        projectFile.getName(),
                        projectFile.getId(),
                        parentNodeName);
            }
        }
        if (projectFile instanceof SecurityAnalysisRunner) {
            if (afsProperties.isLaunchRunner()) {
                LOGGER.info("AFS Event: Run a computation on runnerName={}, Id={}, ParentNode={}",
                        projectFile.getName(),
                        projectFile.getId(),
                        parentNodeName);
                ((SecurityAnalysisRunner) projectFile).run();
            } else {
                saveSecurityAnalysisRunnerResultsWithoutItesla(projectFile.getId());
                LOGGER.info("Apogee-Sea: The computation didn't start because the launchRunner value of runnerName={}, Id={}, ParentNode={} is false",
                        projectFile.getName(),
                        projectFile.getId(),
                        parentNodeName);
            }
        }
    }


    /**
     * <p>After receiving the calculation notification from iTesla, save the results</p>
     *
     * @param nodeEvent The nodeEvent object received by ApogeeAfsAppStorageListener.
     */
    private void saveSecurityAnalysisRunnerResults(NodeEvent nodeEvent) {
        Optional<ComputationData> computationDataOptional = computationDataRepository.findByIdAfsSecurityAnalysisRunner(nodeEvent.getId());
        ComputationData computationData;
        if (computationDataOptional.isPresent()) {
            computationData = computationDataOptional.get();
            if (computationData.getStatus().equals(ExecStatus.RUNNING)) {
                ProjectFile projectFile = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(nodeEvent.getId(), ProjectFile.class);
                SecurityAnalysisResult securityAnalysisResult = null;
                VariantSimulatorResult variantSimulatorResult = null;

                Optional<AbstractComputationResult> abstractComputationResultOptional = networkRepository.getComputationResultRepository().findByIdAfsRunner(projectFile.getId());
                AbstractComputationResult computationResult;
                if (abstractComputationResultOptional.isPresent()) {
                    computationResult = abstractComputationResultOptional.get();

                    if (projectFile instanceof SecurityAnalysisProcessRunner) {
                        SecurityAnalysisProcessRunner runnerProcess = (SecurityAnalysisProcessRunner) projectFile;
                        SecurityAnalysisProcessResult securityAnalysisProcessResult = runnerProcess.readResult();
                        variantSimulatorResult = securityAnalysisProcessResult.getVariantSimulatorResult();
                        saveVariantSimulatorResult(computationResult, computationData, variantSimulatorResult);
                    }

                    if (projectFile instanceof SecurityAnalysisRunner) {
                        SecurityAnalysisRunner runner = (SecurityAnalysisRunner) projectFile;
                        securityAnalysisResult = runner.readResult();
                        saveComputationResult(computationResult, computationData, securityAnalysisResult);
                    }
                }
            }
        }
    }

    private void saveSecurityAnalysisRunnerResultsWithoutItesla(String runnerId) {
        Optional<ComputationData> computationDataOptional = computationDataRepository.findByIdAfsSecurityAnalysisRunner(runnerId);
        ComputationData computationData = null;
        if (computationDataOptional.isPresent()) {
            computationData = computationDataOptional.get();
        }
        SecurityAnalysisResult securityAnalysisResult = null;
        try {
            Path securityAnalysisResultPath = utilityAfs.getPathOfTheFile("security_analysis_result.json");
            securityAnalysisResult = SecurityAnalysisResultDeserializer.read(securityAnalysisResultPath);
        } catch (Exception e) {
            LOGGER.error("Error while parsing the security analysis result", e);
            return;
        }

        Optional<AbstractComputationResult> abstractComputationResultOptional = networkRepository.getComputationResultRepository().findByIdAfsRunner(runnerId);
        AbstractComputationResult computationResult;

        if (abstractComputationResultOptional.isPresent()) {
            computationResult = abstractComputationResultOptional.get();
            saveComputationResult(computationResult, computationData, securityAnalysisResult);
        }
    }

    private void saveComputationResult(AbstractComputationResult computationResult, ComputationData computationData, SecurityAnalysisResult securityAnalysisResult) {
        NetworkContext networkContext = computationResult.getNetworkContext();
        if (computationResult instanceof NetworkSecurityAnalysisResult) {
            Instant endDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            computationResult.setEndDate(endDate);
            computationResult.setExecStatus(ExecStatus.COMPLETED);
            computationData.setStatus(ExecStatus.COMPLETED);
            if (securityAnalysisResult != null) {
                NetworkLimitViolationsResult networkLimitViolationsResult = ((NetworkSecurityAnalysisResult) computationResult).getPreContingencyResult();
                convertorDataComputationResult.populateNetworkLimitViolationsResult(networkLimitViolationsResult, securityAnalysisResult.getPreContingencyResult(), true);
                convertorDataComputationResult.populateNetworkPostContingencyResultList((NetworkSecurityAnalysisResult) computationResult, securityAnalysisResult.getPostContingencyResults());
                networkRepository.getComputationResultRepository().saveAndFlush(computationResult);
                LOGGER.info("Apogee-Sea: Saving a data of computation result afsRunnerId={} for network context type={}, networkDate={}, computationDate={}",
                        computationResult.getIdAfsRunner(),
                        networkContext.getCaseType().getName(),
                        networkContext.getNetworkDate(),
                        networkContext.getComputationDate());
                computationDataRepository.delete(computationData);

                //contact remedials repository
                try {
                    remedialsService.retrieveAndSaveRemedials(securityAnalysisResult, computationResult.getId());
                } catch (IRemedialsService.RemedialServiceException e) {
                    LOGGER.error("Apogee-Sea: Error while retrieving the remedials for network context type={}, networkDate={}, computationDate={}",
                            networkContext.getCaseType().getName(), networkContext.getNetworkDate(),
                            networkContext.getComputationDate(), e);
                }

                if (networkContext.getCaseType().getCaseCategory().isTriggerUiSnapshot()) {
                    uiSnapshotDaoImpl.handleUiSnapshotCreation();
                }
            }
        }
    }


    private void saveVariantSimulatorResult(AbstractComputationResult computationResult, ComputationData computationData, VariantSimulatorResult variantSimulatorResult) {
        NetworkContext networkContext = computationResult.getNetworkContext();
        if (computationResult instanceof NetworkSecurityAnalysisResult) {
            Instant endDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
            computationResult.setEndDate(endDate);
            computationResult.setExecStatus(ExecStatus.COMPLETED);
            computationData.setStatus(ExecStatus.COMPLETED);
            if (variantSimulatorResult != null) {

                convertorDataComputationResult.populateNetworkVariantSimulatorResult((NetworkSecurityAnalysisResult) computationResult, variantSimulatorResult);
                networkRepository.getComputationResultRepository().saveAndFlush(computationResult);
                LOGGER.info("Apogee-Sea: Saving a data of computation result afsRunnerId={} for network context type={}, networkDate={}, computationDate={}",
                        computationResult.getIdAfsRunner(),
                        networkContext.getCaseType().getName(),
                        networkContext.getNetworkDate(),
                        networkContext.getComputationDate());
                computationDataRepository.delete(computationData);

                //contact remedials repository
                try {
                    remedialsService.retrieveAndSaveRemedials(variantSimulatorResult, computationResult.getId());
                } catch (IRemedialsService.RemedialServiceException e) {
                    LOGGER.error("Apogee-Sea: Error while retrieving the remedials for network context type={}, networkDate={}, computationDate={}",
                            networkContext.getCaseType().getName(), networkContext.getNetworkDate(),
                            networkContext.getComputationDate(), e);
                }

                if (networkContext.getCaseType().getCaseCategory().isTriggerUiSnapshot()) {
                    uiSnapshotDaoImpl.handleUiSnapshotCreation();
                }
            }
        }
    }
}
