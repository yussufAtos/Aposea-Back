FROM rtsi.inca.rte-france.com/oraclejdk:1.8.0_74

ENV SLEEP 0
RUN localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
ENV LANG en_US.UTF-8

# add directly the jar
COPY ./web-server/target/*.jar /applis/apogee/apogee-sea-web-server.jar

#create group and user apogee
RUN groupadd -g 2000 -r apogee \
&& useradd -u 2000 -g apogee -c 'User apogee' apogee \
&& mkdir -p /applis/apogee/logs \
&& chown -R apogee:apogee /applis/apogee

USER apogee

CMD echo "The application will start in ${SLEEP}s..." && \
    sleep ${SLEEP} && \
    cd /applis/apogee && \
    java -Djava.security.egd=file:/dev/./urandom $SEA_JAVA_OPTS -jar /applis/apogee/apogee-sea-web-server.jar
