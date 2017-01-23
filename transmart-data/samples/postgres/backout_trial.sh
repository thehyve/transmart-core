#!/bin/bash
#set -x
#set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="study"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

echo "Info: backout on study ${STUDY_ID}"

$PGSQL_BIN/psql <<_END
        select tm_cz.i2b2_backout_trial(trialid:='${STUDY_ID}',path_string:=null,currentjobid:=0);
        -- select tm_cz.i2b2_backout_trial('${STUDY_ID}', null, 0);
_END

