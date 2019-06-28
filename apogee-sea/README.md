[![Coverage](https://devin-qualite.rte-france.com/api/badges/measure?key=com.rte_france.apogee%3Aapogee-sea-backend&metric=coverage)](https://devin-qualite.rte-france.com/component_measures/metric/coverage/list?id=com.rte_france.apogee%3Aapogee-sea-backend) [![Quality Gate](https://devin-qualite.rte-france.com/api/badges/gate?key=com.rte_france.apogee%3Aapogee-sea-backend)](https://devin-qualite.rte-france.com/dashboard?id=com.rte_france.apogee%3Aapogee-sea-backend)


# Environment Requirements

* JDK (8 or greater)
* Maven

Maven configuration to use RTE Nexus (Linux: ```~/.m2/settings.xml```) :
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <mirrors>
        <mirror>
            <id>devin-depot</id>
            <mirrorOf>*</mirrorOf>
            <url>https://devin-depot.rte-france.com/repository/maven-public/</url>
        </mirror>
    </mirrors>
</settings>
```

# 'Convenience' Requirements
* docker (1.12.6)
* docker-compose (1.11.2)

Docker configuration to be able to pull images behind RTE's proxy:

create the file ``` /etc/systemd/system/docker.service.d/http-proxy.conf```
with the following content

```
[Service]
Environment="HTTP_PROXY=http://<NNI>:<PASSWORD-URL-ENCODED>@proxy-surf.rte-france.com:3128/" "NO_PROXY=localhost,127.0.0.0/8, rtsi.inca.rte-france.com, app.inca.rte-france.com, app-prod.inca.rte-france.com"
```

To apply changes
```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

# Install
Normal Maven goals
```bash
mvn clean install
#or without tests
mvn clean install -DskipTests
```

# Execution prérequisites

## PostGreSQL database server
Just use the provided docker-compose.yml

```bash
# pull the images
docker-compose pull

# create and start the containers
docker-compose up
#or in detached mode:
docker-compose up -d
```

## AFS server and iTesla-AS server (PowSyBl itools configuration)
Here a sample config file to use the integration environment. (File: ```~/.itools/config.yml```)

```yaml
remote-service:
    host-name:                             afs-sea-dev.rte-france.com
    app-name:                              sea-afs-server
    secure:                                false
    port:                                  8080

remote-security-analysis-process-service:
    host-name:                             itesla-as-dev.rte-france.com
    port:                                  80
    username:                              admin
    password:                              admin_itesla1234
    secure:                                false

remote-security-analysis-service:
    host-name:                             itesla-as-dev.rte-france.com
    port:                                  80
    username:                              admin
    password:                              admin_itesla1234
    secure:                                false

rest-app-file-system:
  url-address:                             http://afs-sea-dev.rte-france.com:8080/sea-afs-server/

load-flow-action-simulator:
  load-flow-factory:                       com.rte_france.powsybl.hades2.Hades2Factory
  max-iterations:                          3
  ignore-pre-contingency-violations:       true
```

# Execution hello wprd

## Starting the application
```bash
java -jar web-server/target/apogee-sea-web-server-1.0.0-SNAPSHOT.jar
```

To use your own AFS projectFolder instead of default SEA_ROOT (you MUST do this):
```bash
java -jar web-server/target/apogee-sea-web-server-1.0.0-SNAPSHOT.jar --afs.sea.projectFolder=JOHNDOE_ROOT
```

## Inserting cases to AFS
See this project : https://devin-source.rte-france.com/sea/sea-afs-scripts
