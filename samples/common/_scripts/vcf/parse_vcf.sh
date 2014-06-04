# Check input parameters
if [ $# -lt 7 ]
  then
    echo "No or invalid arguments supplied."
    
    echo "Usage: ./parse_vcf.sh vcf_input_file output_dir datasource dataset_id gpl_id genome_build ETL_user"
    echo "    vcf_input_file is the VCF file you want to load"
	echo "    output_dir is a writable directory where the output files will be stored"
    echo "    datasource is a textual description of the source of the data"
    echo "    dataset_id is a unique dataset identifier for this dataset"
    echo "    gpl_id is an identifier for the platform to use. "
    echo "        A platform for VCF currently only describes the genome build. If unsure, use 'VCF_<genome_build>'"
    echo "    genome_build is an identifier for the genome build used as a reference."
    echo "    ETL_user is a textual description of the person loading the data. For example the initials."
    echo ""
    echo "Example: ./parse_vcf.sh 54genomes_chr17_10genes.vcf /tmp/vcf CGI 54GenomesChr17 VCF_HG19 hg19 HW transmart\n"

    exit 1
fi

perl parseVCFintoTextFiles.pl $1 $2 $3 $4 $5 $6