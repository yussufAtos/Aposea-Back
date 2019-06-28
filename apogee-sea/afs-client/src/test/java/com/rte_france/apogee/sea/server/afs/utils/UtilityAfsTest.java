package com.rte_france.apogee.sea.server.afs.utils;


import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.action.dsl.afs.ActionScriptBuilder;
import com.powsybl.afs.*;
import com.powsybl.afs.ext.base.ImportedCase;
import com.powsybl.afs.ext.base.ImportedCaseBuilder;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import com.rte_france.apogee.sea.server.afs.AfsForTests;
import com.rte_france.apogee.sea.server.afs.AfsProperties;
import com.rte_france.apogee.sea.server.afs.exceptions.ApogeeAfsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ListenableAppStorage.class, UtilityAfsTest.class, AfsProperties.class,
        AppDataBean.class})
@TestPropertySource(
        locations = "classpath:apogeetest.properties")
public class UtilityAfsTest {

    static final String TEST_FS_NAME = "computations";
    static final String TEST_ROOT_FOLDER_NAME = "SEA_ROOT";
    static final String TEST_PATH_CONFIG = "config/defauts";

    @Autowired
    private AfsProperties afsProperties;

    private AppDataBean appDataBean;

    private UtilityAfs utilityAfs = new UtilityAfs(afsProperties, appDataBean);

    private AppFileSystem afs;

    private ListenableAppStorage listenableAppStorage;


    @BeforeEach
    public void setUp() {
        utilityAfs = new UtilityAfs(afsProperties, appDataBean);

        afsProperties = Mockito.mock(AfsProperties.class);
        Mockito.when(afsProperties.getFileSystemName())
                .thenReturn(TEST_FS_NAME);
        Mockito.when(afsProperties.getProjectFolder())
                .thenReturn(TEST_ROOT_FOLDER_NAME);
        Mockito.when(afsProperties.getPathContingencies())
                .thenReturn(TEST_PATH_CONFIG);

        AppData appData = AfsForTests.createMemAppData();
        afs = appData.getFileSystem(TEST_FS_NAME);
        listenableAppStorage = AfsForTests.listenableAppStorage;

        appDataBean = Mockito.mock(AppDataBean.class);
        Mockito.when(appDataBean.getFileSystem(TEST_FS_NAME))
                .thenReturn(afs);

        utilityAfs.setAppDataBean(appDataBean);
        utilityAfs.setAfsProperties(afsProperties);
    }

    @AfterEach
    public void tearDown() throws IOException {
        Folder folder = afs.getRootFolder();
        for (Node node : folder.getChildren()) {
            this.listenableAppStorage.deleteNode(node.getId());
        }
        this.listenableAppStorage.close();
    }


    @Test
    public void getConfigFolderInfoTest() {
        String[] pathContingencies = utilityAfs.getAfsProperties().getPathContingencies().split("/");
        Folder folder = utilityAfs.getAppDataBean().getFileSystem(TEST_FS_NAME).getRootFolder();
        Project projetSeaRoot = folder.createProject(utilityAfs.getAfsProperties().getProjectFolder());
        ProjectFolder rootSeaRootFolder = projetSeaRoot.getRootFolder();
        rootSeaRootFolder.createFolder(pathContingencies[0]);
        Optional<NodeInfo> configFolderInfoTest = listenableAppStorage.getChildNode(rootSeaRootFolder.getId(), pathContingencies[0]);
        assertNotNull(configFolderInfoTest);
        assertEquals(configFolderInfoTest.get().getId(), utilityAfs.getConfigFolderInfo(rootSeaRootFolder, listenableAppStorage).get().getId());

    }


    @Test
    public void injectPropertiesTest() {
        assertEquals(TEST_FS_NAME, utilityAfs.getAfsProperties().getFileSystemName());
        assertEquals(TEST_ROOT_FOLDER_NAME, utilityAfs.getAfsProperties().getProjectFolder());
        assertEquals(TEST_PATH_CONFIG, utilityAfs.getAfsProperties().getPathContingencies());
    }

    public void getSeaRootFolderWithExceptionTest() {
        Assertions.assertThrows(ApogeeAfsException.class, () -> {
            utilityAfs.getSeaRootFolder();

        });
    }

    @Test
    public void getSeaRootFolderTest() {
        Folder folder = afs.getRootFolder();
        Project projet = folder.createProject(utilityAfs.getAfsProperties().getProjectFolder());

        assertEquals(projet.getRootFolder().getName(), utilityAfs.getSeaRootFolder().getName());
        assertEquals(projet.getName(), utilityAfs.getSeaRootFolder().getProject().getName());
        assertTrue(utilityAfs.getSeaRootFolder().isFolder());
    }


    @Test
    public void isChildFolderSeaRootTest() {
        Folder folder = afs.getRootFolder();
        Project projetJoha = folder.createProject("Joha");
        Project projetSeaRoot = folder.createProject(utilityAfs.getAfsProperties().getProjectFolder());

        ProjectFolder projectFolderIn = projetJoha.getRootFolder();
        assertFalse(utilityAfs.isChildFolderSeaRoot(projectFolderIn));

        projectFolderIn = projetSeaRoot.getRootFolder().createFolder("caseFolder");
        assertEquals(TEST_ROOT_FOLDER_NAME, projectFolderIn.getParent().get().getProject().getName());
        assertTrue(utilityAfs.isChildFolderSeaRoot(projectFolderIn));


        ProjectFolder projectFolderOut = projetJoha.getRootFolder().createFolder("caseFolder");
        assertFalse(utilityAfs.isChildFolderSeaRoot(projectFolderOut));
    }


    @Test
    public void importContingenciesInCaseFolderTest() {
        String[] pathContingencies = utilityAfs.getAfsProperties().getPathContingencies().split("/");
        Folder folder = utilityAfs.getAppDataBean().getFileSystem(TEST_FS_NAME).getRootFolder();
        Project projetSeaRoot = folder.createProject(utilityAfs.getAfsProperties().getProjectFolder());
        ProjectFolder rootSeaRootFolder = projetSeaRoot.getRootFolder();
        ProjectFolder configFolder = rootSeaRootFolder.createFolder(pathContingencies[0]);
        ActionScript actionScriptExcepted = createActionScript(configFolder, pathContingencies[1]);
        ProjectFolder caseFolder = rootSeaRootFolder.createFolder("caseFolder");
        utilityAfs.importContingenciesInCaseFolder(caseFolder, listenableAppStorage.getNodeInfo(configFolder.getId()), listenableAppStorage);
        Optional<NodeInfo> nodeInfo = listenableAppStorage.getChildNode(caseFolder.getId(), pathContingencies[1]);
        assertTrue(nodeInfo.isPresent());
    }

    @Test
    public void buildSecurityAnalysisRunnerTest() {
        String[] pathContingencies = utilityAfs.getAfsProperties().getPathContingencies().split("/");
        Folder root = afs.getRootFolder();

        //create SEA_ROOT project
        Project projetSeaRoot = root.createProject(utilityAfs.getAfsProperties().getProjectFolder());
        ProjectFolder rootSeaRootFolder = projetSeaRoot.getRootFolder();

        ProjectFolder caseFolder = rootSeaRootFolder.createFolder("caseFolder");

        ImportedCase importedCase = caseFolder.fileBuilder(ImportedCaseBuilder.class)
                .withName("test")
                .withDatasource(new ReadOnlyMemDataSource("test.net"))
                .build();

        ActionScript actionScriptExcepted = createActionScript(caseFolder, pathContingencies[1]);

        assertTrue(caseFolder.getChildren().size() == 2);

        utilityAfs.buildSecurityAnalysisRunner(caseFolder, importedCase, actionScriptExcepted);
        assertTrue(caseFolder.getChildren().size() == 3);


    }

    private ActionScript createActionScript(ProjectFolder folder, String name) {
        // create contingency list
        return folder.fileBuilder(ActionScriptBuilder.class)
                .withName(name)
                .withContent(String.join(System.lineSeparator(),
                        "contingency('c1') {",
                        "    equipments 'l1'",
                        "}",
                        ""))
                .build();
    }


}
