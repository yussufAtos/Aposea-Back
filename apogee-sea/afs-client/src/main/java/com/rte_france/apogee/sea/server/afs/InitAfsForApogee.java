package com.rte_france.apogee.sea.server.afs;

import com.powsybl.afs.AppFileSystem;
import com.powsybl.afs.Folder;
import com.powsybl.afs.Project;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.events.AppStorageListener;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
public class InitAfsForApogee {

    private static final Logger LOGGER = LoggerFactory.getLogger(InitAfsForApogee.class);

    private AfsProperties afsProperties;

    private AppDataBean appDataBean;

    private AppStorageListener afsAppStorageListener;

    @Autowired
    public InitAfsForApogee(AfsProperties afsProperties, AppDataBean appDataBean, AppStorageListener afsAppStorageListener) {
        this.afsProperties = afsProperties;
        this.appDataBean = appDataBean;
        this.afsAppStorageListener = afsAppStorageListener;
    }


    public void run() {
        ListenableAppStorage storage = appDataBean.getAppData().getRemotelyAccessibleStorage(afsProperties.getFileSystemName());
        LOGGER.info("InjectionProperty fileSystem= {}", afsProperties.getFileSystemName());
        LOGGER.info("InjectionProperty projectFolderRoot= {}", afsProperties.getProjectFolder());
        LOGGER.info("InjectionProperty folderDefauts= {}", afsProperties.getPathContingencies());

        AppFileSystem appFileSystem = appDataBean.getFileSystem(afsProperties.getFileSystemName());
        Folder folder = appFileSystem.getRootFolder();
        Optional<Project> project = folder.getChild(Project.class, afsProperties.getProjectFolder());

        if (project.isPresent()) {
            LOGGER.info("Project {} already there", afsProperties.getProjectFolder());
        } else {
            folder.createProject(afsProperties.getProjectFolder());
            LOGGER.info("Creating project {}", afsProperties.getProjectFolder());
        }
        storage.addListener(afsAppStorageListener);
    }

}
