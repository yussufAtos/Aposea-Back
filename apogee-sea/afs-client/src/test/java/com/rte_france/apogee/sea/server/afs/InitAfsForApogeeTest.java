package com.rte_france.apogee.sea.server.afs;

import com.powsybl.afs.AppData;
import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.Folder;
import com.powsybl.afs.Project;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {InitAfsForApogeeTest.class, AfsProperties.class})
@TestPropertySource(
        locations = "classpath:apogeetest.properties")
public class InitAfsForApogeeTest {

    static final String TEST_FS_NAME = "computations";
    static final String TEST_ROOT_FOLDER_NAME = "SEA_ROOT";
    static final String TEST_PATH_CONFIG = "config/defauts";

    private AppFileSystem afs;

    private ListenableAppStorage listenableAppStorage;


    @Autowired
    private AfsProperties afsProperties;


    private AppDataBean appDataBean;


    @Mock
    private AppStorageListener afsAppStorageListener;

    private InitAfsForApogee initAfsForApogee;


    @BeforeEach
    public void setUp() {

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
        Mockito.when(appDataBean.getAppData())
                .thenReturn(appData);
        Mockito.when(appDataBean.getFileSystem(TEST_FS_NAME))
                .thenReturn(afs);

        initAfsForApogee = new InitAfsForApogee(afsProperties, appDataBean, afsAppStorageListener);


    }

    @Test
    public void test() {
        assertEquals(true, null != initAfsForApogee);
        initAfsForApogee.run();
        Folder folder = afs.getRootFolder();
        Project projetJoha = folder.createProject(TEST_ROOT_FOLDER_NAME);
        initAfsForApogee.run();

    }
}
