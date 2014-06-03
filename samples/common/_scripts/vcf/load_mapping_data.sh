# Check input parameters
if [ $# -lt 5 ]
  then
    echo "Usage: ./load_mapping_data.sh subject_sample_mapping_file study_id dataset_id fullpath(separated by +) dbname"
    echo "    subject_sample_mapping_file is a tabseparated file with"
    echo "        subject_id as known in clinical data in the first column"
    echo "        sample_id as known in VCF file in the second column"
    echo "    study_id is the study identifier used in clinical data"
    echo "    dataset_id is the dataset identifier also used in loading the VCF data itself"
    echo "    fullpath is the path in the dataset explorer tree where the VCF node should appear."
    echo "        The path must be delimited by + sign. Use quotation marks if the path contains whitespace"
    echo "    dbname is the name of the database to put the data into"
    echo ""
    echo "Example: ./load_mapping_data.sh subject_sample.txt GSE8581 GSE8581_Lung \"Public Studies+GSE8581+Exome Sequencing\" transmart"
    exit 1
fi

perl generate_VCF_mapping_files.pl "$1" "$2" "$3" "$4" "$5"

./load_mapping_tables.sh
