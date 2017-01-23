#!/bin/sh
#set -x
#set -e

# General optional parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID
# Specific mandatory parameters for this upload script:
#   PLATFORM_ID

# locate this shell script, and source a generic shell script to process all params related settings
UPLOAD_SCRIPTS_DIRECTORY=$(dirname "$0")
UPLOAD_DATA_TYPE="annotation"
source "$UPLOAD_SCRIPTS_DIRECTORY/process_params.inc"

PLATFORM_EXISTS=`$PGSQL_BIN/psql -c "select exists (select platform from deapp.de_gpl_info where platform = '$PLATFORM_ID')" -tA`
if [ $PLATFORM_EXISTS = "f" ]
then
    echo "Warning: chromosomal region annotation platform $PLATFORM_ID does not exist ..."
    # Although platform definition (gpl_info record) does not exist, still try to remove chromosal region annotation records
fi

PLATFORM_REFERENCED=`$PGSQL_BIN/psql -c "select exists (select gpl_id from deapp.de_subject_sample_mapping where gpl_id = '$PLATFORM_ID')" -tA`
if [ $PLATFORM_REFERENCED = "t" ]
then
    REFERENCED_BY_STUDY=`$PGSQL_BIN/psql -c "select distinct trial_name from deapp.de_subject_sample_mapping where gpl_id = '$PLATFORM_ID'" -tA`
    echo "Error: chromosomal region annotation platform $PLATFORM_ID still referenced in studies $REFERENCED_BY_STUDY ..."
    exit 1
fi

# Delete chromosomal region annotation platform definition and data
$PGSQL_BIN/psql <<_END
    select count(*) from deapp.de_gpl_info where platform = '${PLATFORM_ID}';
    select count(*) from deapp.de_chromosomal_region where gpl_id = '${PLATFORM_ID}';

    delete from deapp.de_chromosomal_region where gpl_id = '${PLATFORM_ID}';
    delete from deapp.de_gpl_info where platform = '${PLATFORM_ID}';
_END

