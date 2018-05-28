FROM jboss/wildfly:10.0.0.Final

# configuration environment variables
ENV ADMIN_PASSWORD="admin" \
    TRANSMART_URL="http://localhost:8080/transmart-server" \
    DB_HOST="transmart-database" \
    DB_PORT="5432" \
    DB_NAME="transmart" \
    DB_USER="biomart_user" \
    DB_PW="biomart_user" \
    SOLR_HOST="solr" \
    SOLR_PORT="8983"

# non-overridable environment variables
ENV JBOSS_WAR_DEPLOYMENTS="$JBOSS_HOME/standalone/deployments" \
    TRANSMART_SRC_DIR="/tmp/transmart-src" \
    JBOSS_USER_HOME="/opt/jboss" \
    DB_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"

# pre-requisites: packages & wildfly config; copy sources
USER root
RUN sed -i 's/Xmx512m/Xmx2048m/g' $JBOSS_HOME/bin/standalone.conf && \
    sed -i 's/MaxMetaspaceSize=256m/MaxMetaspaceSize=1024m/g' $JBOSS_HOME/bin/standalone.conf && \
    yum update -y && \
    yum -y install wget git centos-release-scl grails groovy postgresql make && \
    yum -y install rh-maven35 && \
    yum clean all && \
    wget https://services.gradle.org/distributions/gradle-2.13-bin.zip -O /tmp/gradle-bin.zip && \
    mkdir /opt/gradle && \
    unzip -d /opt/gradle /tmp/gradle-bin.zip && \
    rm -f /tmp/gradle-bin.zip
COPY ./ "$TRANSMART_SRC_DIR"
RUN chown -R jboss:jboss "$TRANSMART_SRC_DIR"
USER jboss
ENV PATH="/opt/gradle/gradle-2.13/bin:$BUILD_SCRIPTS_FOLDER/bin:${PATH}"

# configuration of sources
RUN cp "$TRANSMART_SRC_DIR/docker/transmart-server/entrypoint.sh" "$JBOSS_USER_HOME/" && \
    chmod +x "$JBOSS_USER_HOME/entrypoint.sh" && \
    cp "$TRANSMART_SRC_DIR/docker/transmart-server/postgresql-42.1.4.jar" "$JBOSS_WAR_DEPLOYMENTS/" && \
    mkdir -p "$JBOSS_USER_HOME/searchIndex" "$JBOSS_USER_HOME/.grails/transmartConfig" && \
    cp "$TRANSMART_SRC_DIR/docker/transmart-server/application.groovy" "$JBOSS_USER_HOME/.grails/transmartConfig/application.groovy"

# compile & install
WORKDIR $TRANSMART_SRC_DIR
RUN gradle :transmart-server:bootRepackage
RUN cp "$TRANSMART_SRC_DIR/transmart-server/build/libs/"*.war "$JBOSS_WAR_DEPLOYMENTS/transmart-server.war" && \
    touch "$JBOSS_WAR_DEPLOYMENTS/transmart-server.war.dodeploy" && \
    touch "$JBOSS_WAR_DEPLOYMENTS/postgresql-42.1.4.jar"

# run
EXPOSE 8080 9990
ENTRYPOINT "$JBOSS_USER_HOME/entrypoint.sh"
