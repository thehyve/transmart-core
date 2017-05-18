#!/bin/bash -e

ORACLE_MACHINE=oracle-travis.thehyve.net

function prepare_transmart_data {
  source ~/ts-travis/init.sh
  (set +e; maybe_checkout_project_branch $(travis_get_owner)/transmart-data ~/transmart-data ||
        checkout_project_branch_with_fallback transmart/transmart-data master ~/transmart-data)
  if [[ $? -ne 0 ]]; then
    echo 'Could not checkout transmart-data' >&2
    return 1
  fi
  make -C ~/transmart-data/env ../vars
}

function install_nmap {
  mkdir /tmp/nmap
  cd /tmp/nmap
  for p in libpcap0.8 nmap; do
    apt-get download $p
    dpkg-deb -x $p*.deb .
  done
  export LD_LIBRARY_PATH=/tmp/nmap/usr/lib/x86_64-linux-gnu
  cd -
}
function customize_vars_postgres {
  echo TABLESPACES=~/tablespaces/ >> ~/transmart-data/vars
  echo PGSQL_BIN=$HOME/pg/bin/ >> ~/transmart-data/vars
  echo PGPORT=5433 >> ~/transmart-data/vars
  echo PGUSER=travis >> ~/transmart-data/vars
}
function customize_vars_oracle {
  local readonly port=$1
  echo "export ORACLE=1" >> ~/transmart-data/vars
  echo "export ORAHOST=$ORACLE_MACHINE" >> ~/transmart-data/vars
  echo "export ORAPORT=$port" >> ~/transmart-data/vars
  echo "export ORASID=ORCL" >> ~/transmart-data/vars
  echo "export ORAUSER=system" >> ~/transmart-data/vars
  echo "export ORAPASSWORD=manager" >> ~/transmart-data/vars
  echo "export ORACLE_MANAGE_TABLESPACES=1" >> ~/transmart-data/vars
  echo "export USE_POOL_PROCESS=1" >> ~/transmart-data/vars
  echo "export _JAVA_OPTIONS='-Djava.security.egd=file:///dev/urandom'" >> ~/transmart-data/vars
}
function create_oracle_batchdbproperties {
  local readonly port=$1 user=$2 password=$3
  cat > batchdb.properties <<EOF
batch.jdbc.driver=oracle.jdbc.driver.OracleDriver
batch.jdbc.url=jdbc:oracle:thin:@$ORACLE_MACHINE:$port:ORCL
batch.jdbc.user=$user
batch.jdbc.password=$password
EOF
  echo "Written batchdb.properties ($port, $user, $password)"
}

function run_postgres_prepare {
  source ~/ts-travis/postgresql_cluster.sh

  prepare_transmart_data
  customize_vars_postgres
  source ~/transmart-data/vars

  install_pg 9.4.4
  create_cluster
  start_cluster
  for i in {1..10}; do
    ~/pg/bin/psql -c "ALTER USER travis PASSWORD 'travis'" -d template1 \
      && break || sleep 1
  done

  mkdir -p $TABLESPACES/{biomart,deapp,indx,search_app,transmart}
  PGDATABASE=template1 make -C ~/transmart-data/ddl/postgres/GLOBAL tablespaces
  skip_fix_tablespaces=1 make -C ~/transmart-data -j3 postgres > /dev/null
  cp .batchdb-psql-travis.properties batchdb.properties
  ./gradlew --console plain functionalTestPrepare
}
function run_oracle_prepare {
  local port=
  if [[ -z $ORACLE_SECRET ]]; then
    echo "ORACLE_SECRET not defined" >&2
    exit 1
  fi

  prepare_transmart_data
  install_nmap

  make -C ~/transmart-data/env groovy
  export PATH=~/transmart-data/env:$PATH

  echo "Asking for oracle database"
  if [[ ! -p fifo ]]; then mkfifo fifo; fi
  if [[ ! -p fifo2 ]]; then mkfifo fifo2; fi
  /tmp/nmap/usr/bin/ncat --ssl-verify --ssl --ssl-trustfile .travis-ca.pem -v \
    $ORACLE_MACHINE 55397 < <(echo $ORACLE_SECRET ; cat fifo) > fifo2 &
  read result < fifo2

  if [[ $result =~ ^OK ]]; then
    port="${result##OK }"
    echo "Got port $port"
  else
    echo "Could not get oracle port: $result" >&2
    exit 1
  fi

  customize_vars_oracle $port

  cd ~/transmart-data
  source vars
  echo "Create database in $ORAHOST $ORAPORT with $ORAUSER $ORAPASSWORD"
  make -C data/oracle start_pool &
  echo "Waiting for oracle driver to be downloaded..."
  while ! find ~/transmart-data/lib -name 'ojdbc*' -size +3M | egrep '.*'; do sleep 1; done
  echo "Done"
  make oracle > /dev/null
  cd -

  create_oracle_batchdbproperties $port system manager
  ./gradlew --console plain setupSchema
  ./gradlew --console plain functionalTestPrepare
  create_oracle_batchdbproperties $port tm_cz tm_cz
}

function run_postgres_check {
  ./gradlew --info --console plain check
}
function run_oracle_check {
  echo "Skipped on Oracle (same as in postgres)"
}

function run_postgres_functional {
  # also create test report
  ./gradlew --info --stacktrace --console plain functionalTest jacocoTestReport
}
function run_oracle_functional {
  ./gradlew --info --stacktrace --console plain functionalTest
}

function run_postgres_capsule {
  ./gradlew --console plain capsule
}
function run_oracle_capsule {
  echo "Skipped on Oracle (same as in postgres)"
}


function run {
  local readonly db=$1 phase=$2

  (set -e; eval "run_${db}_$phase";)
}

# vim: et tw=80 ts=2 sw=2:
