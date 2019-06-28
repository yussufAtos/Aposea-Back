package com.rte_france.apogee.sea.server.afs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@PropertySource("classpath:apogee.properties")
@ConfigurationProperties("afs.sea")
public class AfsProperties {
    private String fileSystemName;
    private String projectFolder;
    private String pathContingencies;
    private boolean asynchronousRunner;
    private boolean launchRunner;
    private boolean contingenciesWithRemedial;
}







