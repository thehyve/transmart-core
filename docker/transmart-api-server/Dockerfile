FROM openjdk:8u201-jre-alpine3.9
LABEL maintainer="luk.zim91@gmail.com" \
      service="transmart-api-server" \
      version="17.1-HYVE-5.9-RC1" \
      description="This is the Docker image of the tranSMART API server"

# The port is configured in the docker-entrypoint.sh when writing the config.yml
EXPOSE 8081

ENV TRANSMART_USER_NAME transmart
ENV TRANSMART_GROUP_NAME "${TRANSMART_USER_NAME}"
ENV TRANSMART_USER_HOME "/home/${TRANSMART_USER_NAME}"
ENV TRANSMART_API_SERVER_WAR_URL  https://repo.thehyve.nl/service/local/repositories/releases/content/org/transmartproject/transmart-api-server/17.1-HYVE-5.9-RC1/transmart-api-server-17.1-HYVE-5.9-RC1.war
ENV TRANSMART_SERVICE_WAR_FILE "${TRANSMART_USER_HOME}/transmart-api-server.war"
ENV DOCKERIZE_VERSION v0.6.1

# Root does the following things in this order:
# 1. Copies the entrypoint
# 2. Install dockerize:  https://github.com/jwilder/dockerize
# 3. Upgrade Packages of Alpine Linux
# 4. Adds transmart user and group
# 5. Downloads TranSMART API Server war file
# 6. Sets permissions
# 7. Cleanup
USER root
COPY docker-entrypoint.sh /opt/docker-entrypoint.sh
RUN  wget "https://github.com/jwilder/dockerize/releases/download/${DOCKERIZE_VERSION}/dockerize-alpine-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
     tar -C /usr/local/bin -xzvf "dockerize-alpine-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
     rm "dockerize-alpine-linux-amd64-${DOCKERIZE_VERSION}.tar.gz" && \
     apk update --no-cache -U && \
     apk upgrade --no-cache -U && \
     addgroup -S "${TRANSMART_GROUP_NAME}" && \
     adduser -h "${TRANSMART_USER_HOME}" \
             -G "${TRANSMART_GROUP_NAME}" \
             -S \
             -D \
             "${TRANSMART_USER_NAME}" && \
     wget "${TRANSMART_API_SERVER_WAR_URL}" -O "${TRANSMART_SERVICE_WAR_FILE}" && \
     chown -R "${TRANSMART_USER_NAME}:${TRANSMART_GROUP_NAME}" "${TRANSMART_USER_HOME}" && \
     chown    "${TRANSMART_USER_NAME}:${TRANSMART_GROUP_NAME}" /opt/docker-entrypoint.sh && \
     chmod u+x /opt/docker-entrypoint.sh && \
     rm -rf /tmp/* /var/tmp/* && sync

# Set environment for runtime
USER "${TRANSMART_USER_NAME}"
WORKDIR "${TRANSMART_USER_HOME}"
ENTRYPOINT ["/opt/docker-entrypoint.sh"]
