package com.rte_france.apogee.sea.server.afs;

import com.google.common.collect.ImmutableList;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.action.dsl.afs.ActionScriptBuilder;
import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.Node;
import com.powsybl.afs.ProjectFolder;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.storage.events.NodeCreated;
import com.powsybl.afs.storage.events.NodeDataUpdated;
import com.powsybl.afs.storage.events.NodeEventList;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.BusbarSectionContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.security.*;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.powsybl.security.afs.SecurityAnalysisRunnerBuilder;
import com.rte_france.apogee.sea.server.afs.utils.ConvertorDataComputationResult;
import com.rte_france.apogee.sea.server.afs.utils.UtilityAfs;
import com.rte_france.apogee.sea.server.model.computation.*;
import com.rte_france.apogee.sea.server.model.dao.computation.ComputationDataRepository;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.SnapshotRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.itesla.security.SecurityAnalysisProcessResult;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunnerBuilder;
import com.rte_france.itesla.security.result.LimitViolationBuilder;
import com.rte_france.itesla.security.result.LimitViolations;
import com.rte_france.itesla.variant.result.*;
import com.rte_france.powsybl.hades2.Hades2Factory;
import com.rte_france.powsybl.shortcircuit.FaultResult;
import com.rte_france.powsybl.shortcircuit.ShortCircuitAnalysisResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.FileSystem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ListenableAppStorage.class, ApogeeAfsAppStorageListenerTest.class, ApogeeAfsAppStorageListener.class, AfsProperties.class,
        AppDataBean.class, String.class, UtilityAfs.class, ConvertorDataComputationResult.class, NetworkRepository.class, IRemedialsService.class})

@EnableJpaRepositories(basePackages = "com.rte_france.apogee.sea.server.model")
@EnableAutoConfiguration
@Transactional
@EntityScan("com.rte_france.apogee.sea.server.model")
@ActiveProfiles("test")
public class ApogeeAfsAppStorageListenerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApogeeAfsAppStorageListenerTest.class);

    static {
        // Load the module load-flow-action-simulator in memory instead of the itools config file config.xml
        try {
            FileSystem fileSystem = Jimfs.newFileSystem(Configuration.unix());
            InMemoryPlatformConfig platformConfig = new InMemoryPlatformConfig(fileSystem);
            MapModuleConfig config = platformConfig.createModuleConfig("load-flow-action-simulator");
            config.setClassProperty("load-flow-factory", Hades2Factory.class);
            config.setStringProperty("max-iterations", "3");
            config.setStringProperty("ignore-pre-contingency-violations", "true");
            PlatformConfig.setDefaultConfig(platformConfig);
            fileSystem.close();
        } catch (Exception e) {
            LOGGER.error("Error during initialization of Module load-flow-action-simulator");
        }
    }

    private static final String TEST_FS_NAME = "computations";
    private static final String TEST_ROOT_FOLDER_NAME = "SEA_ROOT";
    private static final String TEST_PATH_CONFIG = "config/defauts";

    private AppFileSystem afs;

    private ListenableAppStorage listenableAppStorage;

    @MockBean
    private AfsProperties afsProperties;

    @MockBean
    private AppDataBean appDataBean;

    @MockBean
    private UiSnapshotDaoImpl uiSnapshotDaoImpl;

    @MockBean
    private SnapshotRepository snapshotRepository;

    @Autowired
    private ComputationDataRepository computationDataRepository;

    @Autowired
    private ConvertorDataComputationResult convertorDataComputationResult;

    @MockBean
    private IRemedialsService remedialService;

    @Autowired
    private NetworkRepository networkRepository;


    private UtilityAfs utilityAfs;

    private ApogeeAfsAppStorageListener apogeeAfsAppStorageListener;

    @BeforeEach
    public void setUp() {
        Mockito.when(afsProperties.getFileSystemName())
                .thenReturn(TEST_FS_NAME);

        Mockito.when(afsProperties.getProjectFolder())
                .thenReturn(TEST_ROOT_FOLDER_NAME);
        Mockito.when(afsProperties.getPathContingencies())
                .thenReturn(TEST_PATH_CONFIG);
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(false);

        AppData appData = AfsForTests.createMemAppData();
        afs = appData.getFileSystem(TEST_FS_NAME);
        listenableAppStorage = AfsForTests.listenableAppStorage;

        Mockito.when(appDataBean.getAppData())
                .thenReturn(appData);
        Mockito.when(appDataBean.getStorage(TEST_FS_NAME))
                .thenReturn(listenableAppStorage);
        Mockito.when(appDataBean.getFileSystem(TEST_FS_NAME))
                .thenReturn(afs);

        UtilityAfs utilityAfs = new UtilityAfs(afsProperties, appDataBean);
        utilityAfs.setAppDataBean(appDataBean);
        utilityAfs.setAfsProperties(afsProperties);


        apogeeAfsAppStorageListener = new ApogeeAfsAppStorageListener(afsProperties, appDataBean, utilityAfs,
                convertorDataComputationResult, computationDataRepository, networkRepository, remedialService);
        this.listenableAppStorage.addListener(apogeeAfsAppStorageListener);

    }

    @AfterEach
    public void tearDown() {
        for (Node node : afs.getRootFolder().getChildren()) {
            this.listenableAppStorage.deleteNode(node.getId());
        }
        this.listenableAppStorage.removeListener(this.apogeeAfsAppStorageListener);
        this.listenableAppStorage.close();
    }

    @Test
    public void testWithSecurityAnalysisRunner() {
        assertNotNull(apogeeAfsAppStorageListener);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        //Create project folder out side SEA-ROOT
        ProjectFolder folderProject1 = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject("OUTSIDE_ROOT").getRootFolder();
        ProjectFolder caseFolder1 = folderProject1.createFolder("caseFolder");

        ImportedCase aCaseOutSide = caseFolder1.fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();
        events.addEvent(new NodeCreated(aCaseOutSide.getId(), caseFolder1.getId()));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> defautsInfoOutside = listenableAppStorage.getChildNode(caseFolder1.getId(), "defauts");
        assertFalse(defautsInfoOutside.isPresent());

        //Create script in SEA_ROOT
        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        //Create imported case in SEA_ROOT
        ImportedCase aCase = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeCreated(aCase.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> defautsInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertFalse(defautsInfo.isPresent());
        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }


        SecurityAnalysisRunner runner = caseFolder.fileBuilder(SecurityAnalysisRunnerBuilder.class)
                .withName("runnerTest")
                .withCase(aCase)
                .withContingencyStore(script)
                .build();

        //Create Security Analysis Runner
        events.addEvent(new NodeDataUpdated(aCase.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);


        Optional<NodeInfo> nodeInfoRunner = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        assertTrue(nodeInfoRunner.isPresent());
        assertThat(nodeInfoRunner.get().getName()).isEqualTo("security-analysis-runner");


        Optional<ComputationData> computationData = computationDataRepository.findByIdAfsSecurityAnalysisRunner(nodeInfoRunner.get().getId());
        if (computationData.isPresent()) {
            ComputationData computationData1 = new ComputationData(runner.getId());
            computationData1.setStatus(ExecStatus.CREATED);
            computationData1.setNetworkContextId(computationData.get().getNetworkContextId());
            computationDataRepository.save(computationData1);
        }

        //Delete Security Analysis Runner
        Optional<NodeInfo> runnerInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        runnerInfo.map(r -> this.listenableAppStorage.deleteNode(r.getId()));


        //Delete defauts file
        Optional<NodeInfo> defautsInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertTrue(defautsInfo1.isPresent());
        assertThat(defautsInfo1.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsInfo1.get().getId());

        //Delete defautsWithIal file
        Optional<NodeInfo> defautsInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "defautsWithIal");
        assertTrue(defautsInfo2.isPresent());
        assertThat(defautsInfo2.get().getName()).isEqualTo("defautsWithIal");
        this.listenableAppStorage.deleteNode(defautsInfo2.get().getId());


        //lancer Security Analysis Runner
        events.addEvent(new NodeDataUpdated(runner.getId(), "parametersJson"));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> runnerInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        assertTrue(runnerInfo1.isPresent());
        assertThat(runnerInfo1.get().getName()).isEqualTo("security-analysis-runner");
        this.listenableAppStorage.deleteNode(runnerInfo1.get().getId());

        runner.writeResult(createSecurityAnalysisResult());
        events.addEvent(new NodeDataUpdated(runner.getId(), "resultJsonV2"));
        apogeeAfsAppStorageListener.onEvents(events);
    }

    @Test()
    public void testWithParametersImmportedCaseValid() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        //Create imported case in SEA_ROOT with empty type
        ImportedCase aCaseParametersImmportedCaseValid = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("WithTypeEmpty")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();
        events.addEvent(new NodeCreated(aCaseParametersImmportedCaseValid.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseParametersImmportedCaseValid.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isNotEmpty()
                .size().isEqualTo(1);
    }


    @Test()
    public void testWithTypeInvalid() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        //Create imported case in SEA_ROOT with empty type
        ImportedCase aCaseWithTypeEmpty = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("WithTypeEmpty")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "")
                .build();
        events.addEvent(new NodeCreated(aCaseWithTypeEmpty.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseWithTypeEmpty.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);
        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isEmpty();
    }

    @Test()
    public void testWithTypeNotExist() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        //Create imported case in SEA_ROOT with empty type
        ImportedCase aCaseWithTypeEmpty = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("WithTypeEmpty")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "fk")
                .build();
        events.addEvent(new NodeCreated(aCaseWithTypeEmpty.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseWithTypeEmpty.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isEmpty();
    }

    @Test()
    public void testWithTypeNotEnabled() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        CaseType caseType = new CaseType("joha");
        CaseCategory caseCategory = new CaseCategory("joha");
        caseType.setCaseCategory(caseCategory);
        caseType.setEnabled(false);
        caseType = networkRepository.getCaseTypeRepository().save(caseType);

        //Create imported case in SEA_ROOT with empty type
        ImportedCase aCaseWithTypeEmpty = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("WithTypeEmpty")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "joha")
                .build();
        events.addEvent(new NodeCreated(aCaseWithTypeEmpty.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseWithTypeEmpty.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isEmpty();
    }

    @Test()
    public void testWithCaseDateLessComputationDate() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();
        //Create imported case in SEA_ROOT with Case Date Less than Computation Date
        ImportedCase aCaseWithCaseDateLessComputationDate = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("WithCaseDateLessComputationDate")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312536564")
                .withParameter("sea.computation_date", "1456312566564")
                .withParameter("sea.computation_type", "pf")
                .build();
        events.addEvent(new NodeCreated(aCaseWithCaseDateLessComputationDate.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseWithCaseDateLessComputationDate.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isEmpty();
    }

    @Test()
    public void testWithDateNotValid() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();

        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        //Create imported case in SEA_ROOT with date not valid
        ImportedCase aCaseWithDateNotValid = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("aCaseWithDateNotValid")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "145631256620g")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeCreated(aCaseWithDateNotValid.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        events.addEvent(new NodeDataUpdated(aCaseWithDateNotValid.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        List<NetworkContext> networkContexts = networkRepository.getNetworkContextRepository().findAll();
        assertThat(networkContexts)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void testWithSecurityAnalysisProcessRunner() {
        Mockito.when(afsProperties.isAsynchronousRunner())
                .thenReturn(true);

        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();

        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");
        ProjectFolder configFolder = folderProject.createFolder("config");
        NodeEventList events = new NodeEventList();


        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();

        ImportedCase aCase = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeCreated(aCase.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> defautsInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertFalse(defautsInfo.isPresent());

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        SecurityAnalysisProcessRunner processRunner = caseFolder.fileBuilder(SecurityAnalysisProcessRunnerBuilder.class)
                .withName("processRunnerTest")
                .withCase(aCase)
                .withActionScript(script)
                .build();

        //Create Security Analysis Process Runner
        events.addEvent(new NodeDataUpdated(aCase.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> nodeInfoRunner = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-process-runner");
        assertTrue(nodeInfoRunner.isPresent());

        Optional<ComputationData> computationData = computationDataRepository.findByIdAfsSecurityAnalysisRunner(nodeInfoRunner.get().getId());
        if (computationData.isPresent()) {
            ComputationData computationData1 = new ComputationData(processRunner.getId());
            computationData1.setStatus(ExecStatus.CREATED);
            computationData1.setNetworkContextId(computationData.get().getNetworkContextId());
            computationDataRepository.save(computationData1);
        }
        Optional<NodeInfo> runnerInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-process-runner");
        runnerInfo2.map(r -> this.listenableAppStorage.deleteNode(r.getId()));


        //Delete defauts file
        Optional<NodeInfo> defautsInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertTrue(defautsInfo1.isPresent());
        assertThat(defautsInfo1.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsInfo1.get().getId());

        //Delete defautsWithIal file
        Optional<NodeInfo> defautsInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "defautsWithIal");
        assertTrue(defautsInfo2.isPresent());
        assertThat(defautsInfo2.get().getName()).isEqualTo("defautsWithIal");
        this.listenableAppStorage.deleteNode(defautsInfo2.get().getId());

        //lancer Security Process Analysis Runner
        events.addEvent(new NodeDataUpdated(processRunner.getId(), "parametersJson"));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> runnerInfo3 = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-process-runner");
        assertTrue(runnerInfo3.isPresent());
        assertThat(runnerInfo3.get().getName()).isEqualTo("security-analysis-process-runner");
        this.listenableAppStorage.deleteNode(runnerInfo3.get().getId());

        processRunner.writeResult(createSecurityAnalysisProcessResult());
        events.addEvent(new NodeDataUpdated(processRunner.getId(), "resultJsonV2"));
        apogeeAfsAppStorageListener.onEvents(events);
    }

    @Transactional
    @Test
    public void importedCaseCreatedProcessingTest() {
        ProjectFolder folderProject = apogeeAfsAppStorageListener.getAppDataBean().getAppData()
                .getFileSystem(TEST_FS_NAME)
                .getRootFolder()
                .createProject(TEST_ROOT_FOLDER_NAME).getRootFolder();
        ProjectFolder caseFolder = folderProject.createFolder("caseFolder");

        NodeEventList events = new NodeEventList();

        String jsonIal = "{\n" +
                "  \"additionalProp1\": {\"idRemedial1\":\"4\", \"rul\":\"1\"}\n" +
                "}";

        Mockito.mock(UtilityAfs.class);
        Instant networkDate = Instant.ofEpochMilli(Long.valueOf("1456312566564"));

        try {
            Mockito.when(remedialService.retrieveIalCodeRemedial(networkDate))
                    .thenReturn(jsonIal);
        } catch (IRemedialsService.RemedialServiceException e) {
            e.printStackTrace();
        }

        //import imported case outside SEA_ROOT
        assertTrue(caseFolder.getParent().isPresent());
        ImportedCase aCase = caseFolder.getParent().get().fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeDataUpdated(aCase.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        Optional<NodeInfo> defautsInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertFalse(defautsInfo.isPresent());

        //import imported case without a config folder and a defauts file
        ImportedCase aCase1 = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test1")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeDataUpdated(aCase1.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        Optional<NodeInfo> defautsInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertTrue(defautsInfo1.isPresent());
        assertThat(defautsInfo1.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsInfo1.get().getId());

        Optional<NodeInfo> defautsWithIalInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "defautsWithIal");
        assertTrue(defautsWithIalInfo.isPresent());
        assertThat(defautsWithIalInfo.get().getName()).isEqualTo("defautsWithIal");
        this.listenableAppStorage.deleteNode(defautsWithIalInfo.get().getId());

        Optional<NodeInfo> runnerInfo = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        assertTrue(runnerInfo.isPresent());
        assertThat(runnerInfo.get().getName()).isEqualTo("security-analysis-runner");
        this.listenableAppStorage.deleteNode(runnerInfo.get().getId());

        //import imported case with a config folder allready exist but not defauts file
        ProjectFolder configFolder = folderProject.createFolder("config");
        ImportedCase aCase2 = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test2")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeDataUpdated(aCase2.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);
        Optional<NodeInfo> defautsInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertTrue(defautsInfo2.isPresent());
        assertThat(defautsInfo2.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsInfo2.get().getId());

        Optional<NodeInfo> defautsWithIalInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "defautsWithIal");
        assertTrue(defautsWithIalInfo1.isPresent());
        assertThat(defautsWithIalInfo1.get().getName()).isEqualTo("defautsWithIal");
        this.listenableAppStorage.deleteNode(defautsWithIalInfo1.get().getId());

        Optional<NodeInfo> runnerInfo1 = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        assertTrue(runnerInfo1.isPresent());
        assertThat(runnerInfo1.get().getName()).isEqualTo("security-analysis-runner");
        this.listenableAppStorage.deleteNode(runnerInfo1.get().getId());

        Optional<NodeInfo> defautsConfigInfo = listenableAppStorage.getChildNode(configFolder.getId(), "defauts");
        assertTrue(defautsConfigInfo.isPresent());
        assertThat(defautsConfigInfo.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsConfigInfo.get().getId());


        //import imported case with a config folder and defauts file allready exist
        ActionScript script = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defauts")
                .withContent("")
                .build();
        ImportedCase aCase3 = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test3")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .withParameter("sea.case_date", "1456312566564")
                .withParameter("sea.computation_date", "1456312566204")
                .withParameter("sea.computation_type", "pf")
                .build();

        events.addEvent(new NodeDataUpdated(aCase3.getId(), "parameters"));
        apogeeAfsAppStorageListener.onEvents(events);

        Optional<NodeInfo> defautsInfo3 = listenableAppStorage.getChildNode(caseFolder.getId(), "defauts");
        assertTrue(defautsInfo3.isPresent());
        assertThat(defautsInfo3.get().getName()).isEqualTo("defauts");
        this.listenableAppStorage.deleteNode(defautsInfo3.get().getId());

        Optional<NodeInfo> defautsWithIalInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "defautsWithIal");
        assertTrue(defautsWithIalInfo2.isPresent());
        assertThat(defautsWithIalInfo2.get().getName()).isEqualTo("defautsWithIal");
        this.listenableAppStorage.deleteNode(defautsWithIalInfo2.get().getId());

        Optional<NodeInfo> runnerInfo2 = listenableAppStorage.getChildNode(caseFolder.getId(), "security-analysis-runner");
        assertTrue(runnerInfo2.isPresent());
        assertThat(runnerInfo2.get().getName()).isEqualTo("security-analysis-runner");
        this.listenableAppStorage.deleteNode(runnerInfo2.get().getId());

        ActionScript script1 = configFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("defautsTests")
                .withContent("")
                .build();
        events.addEvent(new NodeCreated(script1.getId(), caseFolder.getId()));
        apogeeAfsAppStorageListener.onEvents(events);
    }

    public SecurityAnalysisProcessResult createSecurityAnalysisProcessResult() {
        return SecurityAnalysisProcessResult.builder()
                .variantSimulatorResult(createVariantSimulatorResult())
                .shortCircuitAnalysisResult(createShortCircuitAnalysisResult())
                .build();
    }

    /**
     * Result with :
     * - 2 variants for N state, one with 2 violations, one with failed result
     * - 2 variants for contingency "NHV1_NHV2_1", one with 2 violations, one with no more violation
     */
    private VariantSimulatorResult createVariantSimulatorResult() {
        List<LimitViolation> violations = createViolations();
        NetworkMetadata metaData = new NetworkMetadata(EurostagTutorialExample1Factory.create());
        Contingency contingency1 = new Contingency("HV line 1", singletonList(new BranchContingency("NHV1_NHV2_1")));

        // Create N state results, with 2 variants
        StateResult n = StateResult.builder()
                .initialVariant(VariantResult.builder()
                        .computationOk(true)
                        .violations(violations)
                        .build())
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-1")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-2")
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
                        .ruleId("n-rule-3")
                        .variantResult(VariantResult.builder()
                                .computationOk(true)
                                .violations(violations)
                                .build())
                        .build()
                )
                .actionsResult(ActionResult.builder()
                        .ruleId("n-rule-4")
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

    public static ShortCircuitAnalysisResult createShortCircuitAnalysisResult() {
        List<FaultResult> faultResults = new ArrayList<>();
        FaultResult faultResult = new FaultResult("faultResultID", 2);
        faultResults.add(faultResult);
        List<LimitViolation> limitViolations = new ArrayList<>();
        String subjectId = "id";
        LimitViolationType limitType = LimitViolationType.HIGH_SHORT_CIRCUIT_CURRENT;
        float limit = 2000;
        float limitReduction = 1;
        float value = 2500;
        LimitViolation limitViolation = new LimitViolation(subjectId, limitType, limit, limitReduction, value);
        limitViolations.add(limitViolation);
        return new ShortCircuitAnalysisResult(faultResults, limitViolations);
    }

    public SecurityAnalysisResult createSecurityAnalysisResult() {
        LimitViolationsResult preContingencyResult = new LimitViolationsResult(true, emptyList());

        ContingencyElement contingencyElement = new BusbarSectionContingency("NHV1_NHV2_1");
        List<ContingencyElement> elements = new ArrayList<>();
        elements.add(contingencyElement);

        Contingency contingency = new Contingency("HV line 1", elements);

        LimitViolation limitViolation1 = new LimitViolation("NHV1_NHV2_1", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 667.67957f, Branch.Side.ONE);
        LimitViolation limitViolation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "CURRENT", 0, 500.0f, 1.0f, 711.42523f, Branch.Side.TWO);
        List<LimitViolation> limitViolations = new ArrayList<>();

        limitViolations.add(limitViolation1);
        limitViolations.add(limitViolation2);

        List<String> actionsTaken = new ArrayList<>();
        actionsTaken.add("load_shed_300");
        actionsTaken.add("load_shed_200");
        actionsTaken.add("load_shed_300");

        LimitViolationsResult limitViolationsResult = new LimitViolationsResult(true, limitViolations, actionsTaken);

        PostContingencyResult postContingencyResult = new PostContingencyResult(contingency, limitViolationsResult);

        List<PostContingencyResult> postContingencyResults = new ArrayList<>();

        postContingencyResults.add(postContingencyResult);

        return new SecurityAnalysisResult(preContingencyResult, postContingencyResults);
    }

}
