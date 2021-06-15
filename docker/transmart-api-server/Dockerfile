FROM openjdk:8-jre-alpine
LABEL maintainer="luk.zim91@gmail.com" \
      service="transmart-api-server" \
      description="This is the Docker image of the tranSMART API server"

ARG TRANSMART_VERSION="17.2.11"

ENV TRANSMART_USER_NAME transmart
ENV TRANSMART_GROUP_NAME "${TRANSMART_USER_NAME}"
ENV TRANSMART_USER_HOME "/home/${TRANSMART_USER_NAME}"
ENV TRANSMART_SERVICE_WAR_FILE "/transmart-api-server.war"
ENV APP_PORT 8081

EXPOSE ${APP_PORT}

# Root does the following things in this order:
# 1. Copies the entrypoint
# 2. Adds transmart user and group
# 3. Downloads TranSMART API Server war file
# 4. Sets permissions
# 5. Cleanup
USER root
COPY docker-entrypoint.sh /opt/docker-entrypoint.sh
COPY logback.groovy /logback.groovy
RUN  apk add curl && \
     addgroup -S "${TRANSMART_GROUP_NAME}" && \
     adduser -h "${TRANSMART_USER_HOME}" \
             -G "${TRANSMART_GROUP_NAME}" \
             -S \
             -D \
             "${TRANSMART_USER_NAME}" && \
     chown    "${TRANSMART_USER_NAME}:${TRANSMART_GROUP_NAME}" /opt/docker-entrypoint.sh && \
     chmod u+x /opt/docker-entrypoint.sh && \
     chown "${TRANSMART_USER_NAME}" /etc/ssl/certs/java/cacerts && \
     chown "${TRANSMART_USER_NAME}" /etc/ssl/certs/java && \
     chmod 644 /etc/ssl/certs/java/cacerts && \
     rm -rf /tmp/* /var/tmp/* && sync

RUN if echo "${TRANSMART_VERSION}" | grep '\.*-SNAPSHOT$' -; then \
      curl -f -L "https://repo.thehyve.nl/service/local/artifact/maven/redirect?r=snapshots&g=org.transmartproject&a=transmart-api-server&v=${TRANSMART_VERSION}&p=war" -o "${TRANSMART_SERVICE_WAR_FILE}"; \
    else \
      curl -f -L "https://repo.thehyve.nl/service/local/artifact/maven/redirect?r=releases&g=org.transmartproject&a=transmart-api-server&v=${TRANSMART_VERSION}&p=war" -o "${TRANSMART_SERVICE_WAR_FILE}"; \
    fi

# Set environment for runtime
USER "${TRANSMART_USER_NAME}"
WORKDIR "${TRANSMART_USER_HOME}"
ENTRYPOINT ["/opt/docker-entrypoint.sh"]
