FROM ubuntu:trusty

EXPOSE 8983

RUN apt-get update && \
  DEBIAN_FRONTEND=noninteractive apt-get install -y \
    openjdk-7-jdk curl make git php5-cli php5-json 

ENV PGHOST postgres
ENV PGPORT 5432
ENV PGDATABASE postgres
ENV PGUSER postgres
ENV PGPASSWORD password
ENV TABLESPACES bogus
ENV BRANCH master

RUN git clone -b $BRANCH --depth 1 https://github.com/transmart/transmart-data.git /opt/transmart-data && \
  make -C /opt/transmart-data/solr solr_home && \
  curl -f -L -o /usr/local/bin/dumb-init https://github.com/Yelp/dumb-init/releases/download/v1.0.1/dumb-init_1.0.1_amd64 && \
  chmod +x /usr/local/bin/dumb-init

CMD ["/usr/local/bin/dumb-init", "/usr/bin/make", "-C", "/opt/transmart-data/solr", "start"]
