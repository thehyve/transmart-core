FROM postgres:9.5 # 9.6 not supported by transmart-data

MAINTAINER "Piotr Zakrzewski" <piotr@thehyve.nl>
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && \
 apt-get install -y git make php5-cli sudo php5-json curl unzip software-properties-common
RUN ln -snf /bin/bash /bin/sh
RUN echo "deb http://ftp.de.debian.org/debian jessie-backports main" >> /etc/apt/sources.list.d/webupd8team-java.list \
 && apt-get update && apt-get install -y openjdk-8-jdk
RUN curl -s "https://get.sdkman.io" | bash && \
 source "/root/.sdkman/bin/sdkman-init.sh" && \
 sdk install groovy && sdk use groovy # groovy is needed for uploading test data

ENV BRANCH master # change this to use a different branch of transmart-upgrade

RUN git clone -b $BRANCH --depth 1 https://github.com/thehyve/transmart-upgrade.git /tmp/transmart-upgrade

ENV POSTGRES_PASSWORD ''

RUN ln -snf /root/.sdkman/candidates/groovy/current/bin/groovy /bin/groovy
RUN echo "cd /tmp/transmart-upgrade/transmart-data && \
 make -C env ../vars && source /tmp/transmart-upgrade/transmart-data/vars && \
 make postgres_drop || true && \  # drop database if exists, || true will ignore errors
 make postgres_test || true && \
 chown -R postgres:postgres  /var/lib/postgresql && \
 make postgres_test || true && \ # ugly hack follows
 make postgres_drop || true && \
 make postgres_test || true && \
 chown -R postgres:postgres  /var/lib/postgresql && \
 make postgres_test || true && \
 make postgres_drop || true && \
 make postgres_test" >> /docker-entrypoint-initdb.d/01-create.sh
