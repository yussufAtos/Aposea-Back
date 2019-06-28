package com.rte_france.apogee.sea.server.third;

import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.action.dsl.afs.ActionScriptBuilder;
import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import com.rte_france.apogee.sea.server.afs.AfsForTests;
import com.rte_france.apogee.sea.server.afs.AfsProperties;
import com.rte_france.apogee.sea.server.afs.utils.ConvertorDataComputationResult;
import com.rte_france.apogee.sea.server.model.computation.AbstractComputationResult;
import com.rte_france.apogee.sea.server.model.computation.ExecStatus;
import com.rte_france.apogee.sea.server.model.computation.NetworkSecurityAnalysisResult;
import com.rte_france.apogee.sea.server.model.dao.computation.NetworkRepository;
import com.rte_france.apogee.sea.server.model.dao.uisnapshot.UiSnapshotDaoImpl;
import com.rte_france.apogee.sea.server.services.IRemedialsService;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunnerBuilder;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ThirdService.class, ConvertorDataComputationResult.class, UiSnapshotDaoImpl.class, NetworkRepository.class,
        AfsProperties.class})
@EnableAutoConfiguration
@Transactional
@EntityScan("com.rte_france.apogee.sea.server")
@ComponentScan("com.rte_france.apogee.sea.server")
@EnableJpaRepositories("com.rte_france.apogee.sea.server")
public class ThirdServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdServiceTest.class);
    static final String TEST_FS_NAME = "computations";
    static final String FORMAT = "net";

    private AppFileSystem afs;

    private ListenableAppStorage listenableAppStorage;

    @Autowired
    private ThirdService thirdService;

    @Autowired
    private NetworkRepository networkRepository;

    @Autowired
    private AfsProperties afsProperties;

    @MockBean
    private AppDataBean appDataBean;

    @MockBean
    SecurityAnalysisProcessRunner securityAnalysisProcessRunner;

    @MockBean
    private IRemedialsService remedialsService;

    @BeforeEach
    public void setUp() {
        afsProperties = Mockito.mock(AfsProperties.class);
        Mockito.when(afsProperties.getFileSystemName())
                .thenReturn(TEST_FS_NAME);

        AppData appData = AfsForTests.createMemAppData();
        afs = appData.getFileSystem(TEST_FS_NAME);
        listenableAppStorage = AfsForTests.listenableAppStorage;

        Mockito.when(appDataBean.getAppData())
                .thenReturn(appData);
        Mockito.when(appDataBean.getStorage(TEST_FS_NAME))
                .thenReturn(listenableAppStorage);
        Mockito.when(appDataBean.getFileSystem(TEST_FS_NAME))
                .thenReturn(afs);

        appDataBean = Mockito.mock(AppDataBean.class);
        Mockito.when(appDataBean.getFileSystem(TEST_FS_NAME))
                .thenReturn(afs);
    }

    @Test
    public void saveComputationResultTest() {
        MultipartFile multipartFile = new MockMultipartFile("file",
                "20190109T1250Z_20190109T1250Z_pf.json", "application/json", readJsonFromFile("20190109T1250Z_20190109T1250Z_pf.json"));
        String remedial = "{\"BXLIEL61SIRMI\": [\n" +
                "                                         {\n" +
                "                                           \"idLogicContext\": 689,\n" +
                "                                           \"idAbstractLogic\": 8212,\n" +
                "                                           \"shortDescription\": \"Ouverture SIRML4ZTREZ.1\"\n" +
                "                                         },\n" +
                "                                         {\n" +
                "                                           \"idLogicContext\": 688,\n" +
                "                                           \"idAbstractLogic\": 8210,\n" +
                "                                           \"shortDescription\": \"Ouverture BXLIEL41LUCON\"\n" +
                "                                         }\n" +
                "                                       ]\n" +
                "                                     }";
        MultipartFile multipartFileRemedials = new MockMultipartFile("file",
                "remedials.json", "application/json", remedial.getBytes());

        try {
            thirdService.saveComputationResult(multipartFile, multipartFileRemedials);
        } catch (IThirdService.ThirdServiceException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void retrieveAndSaveRemedialsTest() {
        Folder root = afs.getRootFolder();

        //create SEA_ROOT project
        Project projetSeaRoot = root.createProject("SEA_ROOT");
        ProjectFolder rootSeaRootFolder = projetSeaRoot.getRootFolder();
        ProjectFolder caseFolder = rootSeaRootFolder.createFolder("caseFolder");
        ImportedCase importedCase = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .build();

        // create ActionScript list
        ActionScript actionScript = caseFolder.fileBuilder(ActionScriptBuilder.class)
                .withName("actionScript")
                .withContent(String.join(System.lineSeparator(),
                        "contingency('c1') {",
                        "    equipments 'l1'",
                        "}",
                        ""))
                .build();

        // create a security analysis runner that point to imported case
        SecurityAnalysisProcessRunner runner = caseFolder.fileBuilder(SecurityAnalysisProcessRunnerBuilder.class)
                .withName("sap")
                .withCase(importedCase)
                .withActionScript(actionScript)
                .build();

        // run security analysis
        runner.run();

        AbstractComputationResult computationResult = new NetworkSecurityAnalysisResult();
        computationResult.setIdAfsRunner(runner.getId());
        computationResult.setExecStatus(ExecStatus.COMPLETED);
        computationResult.setName("AS_COMMON");
        Instant startDate = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        computationResult.setStartDate(startDate);
        computationResult.setEndDate(startDate.plusSeconds(60));
        computationResult = networkRepository.getComputationResultRepository().save(computationResult);

        try {
            thirdService.retrieveAndSaveRemedials(computationResult.getIdAfsRunner());
        } catch (IThirdService.ThirdServiceException e) {
            e.printStackTrace();
        }
    }

    public byte[] readJsonFromFile(String name) {
        try {
            return Files.readAllBytes(Paths.get(ClassLoader.getSystemResource("json/" + name).toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
