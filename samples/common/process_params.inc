# Requires that the UPLOAD_DATA_TYPE parameter is set and not empty
# Optionally expecting values for the following parameters:
#   DATA_LOCATION, STUDY_NAME, STUDY_ID, SECURITY_REQUIRED, TOP_NODE_PREFIX

if [ -z "$UPLOAD_DATA_TYPE" ]; then
    echo "Error process_params.inc: no value specified for UPLOAD_DATA_TYPE!!"
    exit 1
fi

# Check UPLOAD_DATA_TYPE value
MOLECULAR_DATA_TYPES="rnaseq acgh expression mirna proteomics vcf"
SUPPORTED_DATA_TYPES="study clinical $MOLECULAR_DATA_TYPES annotation"
if [ $(echo $SUPPORTED_DATA_TYPES | grep -c -w $UPLOAD_DATA_TYPE) -ne 1 ] ; then
    echo "Error process_params.inc: this UPLOAD_DATA_TYPE ($UPLOAD_DATA_TYPE) is not supported"
    exit 1
fi

# Before changing to the data location directory determine absolute path name of params file (optional first argument)
if [ ! -z "$1" ]; then
    params=$(readlink -e "$1")
    if [ ! -f "$params" ]; then
        echo "Error $(basename $0): params file ($params) could not be found"
        exit 1
    fi
fi

DATA_LOCATION=${DATA_LOCATION:-$(pwd)} # If not set or empty, assume that $UPLOAD_DATA_TYPE data files are in the current directory
cd "$DATA_LOCATION"

if [ "$UPLOAD_DATA_TYPE" == "study" ] ; then

    # The current directory should be the study directory
    STUDYDIR="$(pwd)"

elif [ "$UPLOAD_DATA_TYPE" == "annotation" ] ; then

    CURDIR="$(pwd)/"
    unset STUDYDIR
    for MOLECULAR_DATA_TYPE in $MOLECULAR_DATA_TYPES ; do
         # The annotation directory can be anywhere within a molecular data type specific subdirectory structure
         # Determine the first level molecular data type from the current directory
         if [ -z $STUDYDIR ] ; then
             STUDYDIR=${CURDIR%/$MOLECULAR_DATA_TYPE/*annotation/*}
             if [ "$STUDYDIR" == "$CURDIR" ] ; then
                 unset STUDYDIR
             else
                 STUDYDIR=$(dirname $STUDYDIR/$MOLECULAR_DATA_TYPE)
             fi
         fi
     done

else

    # Try to locate study directory in case the study data is organized in data type specific subdirectories
    # The study directory is the parent directory of the directory named '$UPLOAD_DATA_TYPE' which is most near to the current directory
    # in the direction of the root directory
    CURDIR="$(pwd)/"
    STUDYDIR=${CURDIR%/$UPLOAD_DATA_TYPE/*}
    if [ "$STUDYDIR" == "$CURDIR" ] ; then
        unset STUDYDIR
    else
        STUDYDIR=$(dirname $STUDYDIR/$UPLOAD_DATA_TYPE)
    fi

fi

# Try to locate the optional study level params file in the root directory of this study
studyparams="study.params"
if [ ! -z "$STUDYDIR" ]; then
    if [ -f "$STUDYDIR/$studyparams" ]; then
        source "$STUDYDIR/$studyparams"
        echo "Info $(basename $0): Sourced params file $STUDYDIR/$studyparams"
    fi
fi

# If a local optional $UPLOAD_DATA_TYPE.params file exists, also source this one
# It may contain parameter values which only apply locally (not study global).
localparams="./$UPLOAD_DATA_TYPE.params"
if [ -f "$localparams" ] ; then
    source $localparams
    echo "Info $(basename $0): Sourced params file $localparams"
fi

# If a params file name is passed as an argument to this script, source it last,
# as its content should overrule default parameter values found in the optional parameter files
if [ ! -z "$params" ]; then
    source "$params"
    echo "Info $(basename $0): Sourced params file $params"
fi

if [ -z "$STUDY_NAME" ]; then
    # If no study name is provided, use STUDY_ID (if available) or derive from study directory name
    if [ -z $STUDY_ID ] ; then
        # assume that study data is organized in data type specific subdirectories and
        # bundled in a directory with the name of the study
        if [ -z "$STUDYDIR" ]; then
	    if [ "$UPLOAD_DATA_TYPE" == "annotation" ]; then
		echo "Warning: could not determine STUDY_NAME or STUDY_ID"
		echo "         that's probably OK for platform definitions."
	    else
                echo 'Error: Expecting either'
                echo '       - the study name parameter (STUDY_NAME) to be set, or'
                echo '       - the study id parameter (STUDY_ID) to be set, or'
                echo '       - the $UPLOAD_DATA_TYPE study data to be organized in a subdirectory named "$UPLOAD_DATA_TYPE"'
                exit 1
	    fi
        else
            STUDY_NAME="$(basename $STUDYDIR)"
            echo "Info $(basename $0): assuming STUDY_NAME=${STUDY_NAME}"
        fi
    else
        STUDY_NAME=$STUDY_ID
        echo "Info $(basename $0): assuming STUDY_NAME=${STUDY_NAME}"
    fi
fi

if [ -z $STUDY_ID ] ; then
    STUDY_ID=$(echo ${STUDY_NAME} | tr 'a-z' 'A-Z')
    echo "Info $(basename $0): assuming STUDY_ID=${STUDY_ID}"
fi
# Because not all upload (pre-processing) scripts handle STUDY_ID in the same way
# (Kettle e.g. capitalizes study id's, R does not and SQL procedure only capitalizes the procedure argument, not the study_id column in the landingzone)
# STUDY_ID is always capitalized
STUDY_ID=$(echo ${STUDY_ID} | tr 'a-z' 'A-Z')

