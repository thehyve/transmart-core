FROM postgres:9.5

MAINTAINER "Gustavo Lopes" <gustavo@thehyve.nl>

ENV POSTGRES_PASSWORD password

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y git make php5-cli php5-json wget && \
  rm -rf /var/lib/apt/lists/*

ENV BRANCH master

RUN git clone -b $BRANCH --depth 1 https://github.com/transmart/transmart-data.git /tmp/transmart-data && \
  wget https://s3.amazonaws.com/thehyve-tools/public/transmart-generic/dump.sql \
    -O /docker-entrypoint-initdb.d/dump.sql && \
  make -C /tmp/transmart-data/env /var/lib/postgresql/tablespaces && \
  echo "PGUSER=postgres PGPASSWORD=postgres PGDATABASE=postgres TABLESPACES=/var/lib/postgresql/tablespaces/ \
    skip_fix_tablespaces=1 make -C /tmp/transmart-data/ddl/postgres -j5 load" \
    > /docker-entrypoint-initdb.d/01-create.sh && \
  echo "rm -rf /tmp/transmart-data" >> /docker-entrypoint-initdb.d/01-create.sh
