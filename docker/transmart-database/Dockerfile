FROM postgres:9.5

ENV DEBIAN_FRONTEND=noninteractive \
    POSTGRES_USER=postgres \
    POSTGRES_PASSWORD=postgres

# install/set up the pre-requisites
RUN echo "deb http://ftp.de.debian.org/debian jessie main contrib non-free jessie-backports" >> /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-get update && apt-get install -y git make php5-cli sudo php5-json curl zip unzip software-properties-common openjdk-8-jdk && \
    ln -snf /bin/bash /bin/sh
RUN curl -s "https://get.sdkman.io" | bash && \
    source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install groovy && sdk use groovy && \
    ln -snf /root/.sdkman/candidates/groovy/current/bin/groovy /bin/groovy && \
    chmod 777 /root && chmod -R 777 /root/.sdkman && \
    mkdir -p /home/postgres && \
    chown postgres:postgres /home/postgres && \
    usermod -d /home/postgres postgres

# copy and prepare transmart sources for test data loading
COPY . /tmp/transmart-core
RUN echo "PGHOST=localhost \
PGDATABASE=transmart \
PGUSER=${POSTGRES_USER} \
PGPASSWORD=${POSTGRES_PASSWORD} \
PGPORT=5432 \
TABLESPACES=/var/lib/postgresql/tablespaces/ \
PGSQL_BIN=/usr/lib/postgresql/9.5/bin/ \
export PGHOST PGPORT PGDATABASE PGUSER PGPASSWORD TABLESPACES PGSQL_BIN" > /tmp/transmart-core/transmart-data/vars
RUN cd /tmp/transmart-core/transmart-data && \
    mkdir -p /var/lib/postgresql/tablespaces/{biomart,deapp,indx,search_app,transmart} && \
    chown -R postgres:postgres /var/lib/postgresql
RUN echo "cd /tmp/transmart-core/transmart-data && \
 source /tmp/transmart-core/transmart-data/vars && \
 make postgres_test" >> /docker-entrypoint-initdb.d/01-create.sh

# hack postgresql docker image to allow transmart loading (allow TCP/IP connections during init phase)
RUN sed -i "s/listen_addresses=''/listen_addresses='*'/g" /usr/local/bin/docker-entrypoint.sh
