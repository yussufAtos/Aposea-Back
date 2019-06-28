package com.rte_france.apogee.sea.server.afs.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.action.dsl.afs.ActionScript;
import com.powsybl.action.dsl.afs.ActionScriptBuilder;
import com.powsybl.afs.*;
import com.powsybl.afs.storage.ListenableAppStorage;
import com.powsybl.afs.storage.NodeInfo;
import com.powsybl.afs.ws.server.utils.AppDataBean;
import com.powsybl.security.afs.SecurityAnalysisRunner;
import com.powsybl.security.afs.SecurityAnalysisRunnerBuilder;
import com.rte_france.apogee.sea.server.afs.AfsProperties;
import com.rte_france.apogee.sea.server.afs.exceptions.ApogeeAfsException;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunner;
import com.rte_france.itesla.security.afs.SecurityAnalysisProcessRunnerBuilder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@Setter
@Getter
public class UtilityAfs {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilityAfs.class);

    private AfsProperties afsProperties;

    private AppDataBean appDataBean;

    @Autowired
    public UtilityAfs(AfsProperties afsProperties, AppDataBean appDataBean) {
        this.afsProperties = afsProperties;
        this.appDataBean = appDataBean;
    }

    @Nullable
    public boolean isChildFolderSeaRoot(ProjectNode caseFolder) {
        if (caseFolder != null && caseFolder.isFolder()) {
            Optional<ProjectFolder> seaRootFolderInfo = caseFolder.getParent();
            if (seaRootFolderInfo.isPresent()) {
                return afsProperties.getProjectFolder().equals(seaRootFolderInfo.get().getProject().getName());
            }
        }
        return false;

    }

    public ProjectFolder getSeaRootFolder() {
        AppFileSystem appFileSystem = appDataBean.getFileSystem(afsProperties.getFileSystemName());
        Folder folder = appFileSystem.getRootFolder();

        Optional<Project> project = folder.getChild(Project.class, afsProperties.getProjectFolder());

        if (project.isPresent()) {
            return project.get().getRootFolder();
        } else {
            throw new ApogeeAfsException(afsProperties.getProjectFolder() + " does not exist in " + afsProperties.getFileSystemName());
        }
    }

    public Optional<String> buildSecurityAnalysisRunner(ProjectFolder toCaseFolder, ProjectFile importedCase, ActionScript actionScript) {
        SecurityAnalysisRunner runner = toCaseFolder.fileBuilder(SecurityAnalysisRunnerBuilder.class)
                .withName("security-analysis-runner")
                .withCase(importedCase)
                .withContingencyStore(actionScript)
                .build();
        LOGGER.info("AFS Event: Created Security Analysis Runner id ={} in ParentNodeName={}",
                runner.getId(),
                toCaseFolder.getName());
        return Optional.of(runner.getId());
    }

    public Optional<String> buildSecurityAnalysisProcessRunner(ProjectFolder toCaseFolder, ProjectFile importedCase, ActionScript actionScript) {
        SecurityAnalysisProcessRunner runner = toCaseFolder
                .fileBuilder(SecurityAnalysisProcessRunnerBuilder.class)
                .withName("security-analysis-process-runner")
                .withCase(importedCase)
                .withActionScript(actionScript)
                .build();
        LOGGER.info("AFS Event: Created Security Analysis Process Runner id ={} in ParentNodeName={}",
                runner.getId(),
                toCaseFolder.getName());
        return Optional.of(runner.getId());
    }


    public String importContingenciesInCaseFolder(ProjectNode toCaseFolder, NodeInfo fromConfigFolderInfo, ListenableAppStorage storage) {
        String[] pathContingencies = afsProperties.getPathContingencies().split("/");
        String fileNameContingencies = pathContingencies[pathContingencies.length - 1];
        Optional<ActionScript> projectNodeOtional = getContingencies(fromConfigFolderInfo, storage);
        String content;
        ActionScript actionScript = null;
        if (projectNodeOtional.isPresent()) {
            content = projectNodeOtional.get().readScript();
            try {
                actionScript = ((ProjectFolder) toCaseFolder).fileBuilder(ActionScriptBuilder.class)
                        .withName(fileNameContingencies)
                        .withContent(content)
                        .build();
                LOGGER.info("AFS Event: Imported Contingencies name={}, id ={} in ParentNodeName={} from folder name={}",
                        actionScript.getName(),
                        actionScript.getId(),
                        toCaseFolder.getName(),
                        fromConfigFolderInfo.getName());
            } catch (AfsException e) {
                throw new ApogeeAfsException("Parent folder already contains a " + fileNameContingencies + " node", e);
            }
            return actionScript.getId();
        }
        return null;
    }

    public ActionScript createContingenciesEmptyInCaseFolder(ProjectNode toCaseFolder) {
        String[] pathContingencies = afsProperties.getPathContingencies().split("/");
        String fileNameContingencies = pathContingencies[pathContingencies.length - 1];
        try {
            ActionScript actionScript = ((ProjectFolder) toCaseFolder).fileBuilder(ActionScriptBuilder.class)
                    .withName(fileNameContingencies)
                    .withContent("")
                    .build();
            LOGGER.info("AFS Event: Create empty contingencies name={}, id ={} in ParentNodeName={}",
                    actionScript.getName(),
                    actionScript.getId(),
                    toCaseFolder.getName());
            return actionScript;
        } catch (AfsException e) {
            throw new ApogeeAfsException("Parent folder already contains a " + fileNameContingencies + " node", e);
        }
    }

    public Optional<NodeInfo> getConfigFolderInfo(ProjectFolder seaRoot, ListenableAppStorage storage) {
        String[] pathContingencies = afsProperties.getPathContingencies().split("/");
        Optional<NodeInfo> configFolderInfo = Optional.empty();
        if (pathContingencies.length == 2) {
            configFolderInfo = storage.getChildNode(seaRoot.getId(), pathContingencies[0]);
        }
        if (configFolderInfo.isPresent()) {
            return configFolderInfo;
        } else {
            return Optional.empty();
        }
    }


    private Optional<ActionScript> getContingencies(NodeInfo fromConfigFolderInfo, ListenableAppStorage storage) {
        String[] pathContingencies = afsProperties.getPathContingencies().split("/");
        Optional<NodeInfo> contingenciesInfo = storage.getChildNode(fromConfigFolderInfo.getId(), pathContingencies[1]);
        if (contingenciesInfo.isPresent()) {
            NodeInfo contingenciesNodeInfo = contingenciesInfo.get();
            ActionScript projectNode = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(contingenciesNodeInfo.getId(), ActionScript.class);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(emptyLine(50));
            }
            return Optional.of(projectNode);
        } else {
            ProjectFolder root = getSeaRootFolder();
            Optional<ProjectNode> pojectNodeOptional = root.getChild(pathContingencies[0]);
            if (pojectNodeOptional.isPresent()) {
                return Optional.of(createContingenciesEmptyInCaseFolder(pojectNodeOptional.get()));
            }
        }
        return Optional.empty();
    }

    public String emptyLine(int length) {
        StringBuilder sb = new StringBuilder(" ");
        for (int i = 0; i < length; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    public void concatenateIalWithDefault(String contingenciesId, String jsonIal) {
        ActionScript actionScript = appDataBean.getFileSystem(afsProperties.getFileSystemName()).findProjectFile(contingenciesId, ActionScript.class);
        Optional<ProjectFolder> caseFolderInfo = actionScript.getParent();
        ProjectFolder toCaseFolder = null;
        Map<String, Object> map;
        if (caseFolderInfo.isPresent()) {
            toCaseFolder = caseFolderInfo.get();
            try {
                ObjectMapper mapper = new ObjectMapper();
                // convert JSON string to Map
                map = mapper.readValue(jsonIal, new TypeReference<Map<String, Object>>() {
                });
                String defaultList = actionScript.readScript();
                StringBuilder sb = new StringBuilder(defaultList + "\n");
                Set<String> keys = map.keySet();
                for (String key : keys) {
                    sb = sb.append(map.get(key) + "\n");
                }
                toCaseFolder.fileBuilder(ActionScriptBuilder.class)
                        .withName("defautsWithIal")
                        .withContent(sb.toString())
                        .build();
            } catch (JsonParseException e) {
                throw new ApogeeAfsException("Failed parsing prioritize remedials json", e);
            } catch (JsonMappingException e) {
                throw new ApogeeAfsException("Failed mapping prioritize remedials json", e);
            } catch (IOException e) {
                throw new ApogeeAfsException("Error while concatenate ial code of remedial with contingencies", e);
            }
        }
    }

    public Path getPathOfTheFile(String name) {
        try {
            return Paths.get(ClassLoader.getSystemResource("json/" + name).toURI());
        } catch (Exception e) {
            throw new ApogeeAfsException("Failed parsing path of json resources", e);
        }
    }
}


