Parse and Load VCF data to tranSMART.

These scripts are a modified version of the original oracle loading scripts, found at
https://github.com/transmart/transmartApp-DB/tree/master/VCF_db_scripts, adjusted
to also work with postgres databases.

Prerequisites:
1. Make sure Perl is installed and working on your machine. The scripts expects a command like
   $ perl script.pl
   to run correctly, so perl must exist on your path.

In order to load your VCF data, follow these steps:

1. Create patient subject-sample mapping file. This file should have two columns
   (tab delimited). Column 1 is the identification of the patient (the SUBJ_ID
   for the study), and column 2 is the sample ID from the VCF file.
   By default, this file should have the name <subject_sample_mapping.txt>
   although you may change the name, as long as the changes are reflected
   in <vcf.params> (see step 2).
   Note: this file is not provided, as it is specific to the VCF data.

2. Alter the parameters in the vcf.params file

3. Source the params file:
   . ./samples/common/_scripts/vcf/vcf.params

4. Source the transmart-data vars file:
   . ./vars

5. Run make samples/{oracle,postgres} load_vcf (from the transmart-data root directory)

Other command available for both oracle and postgres are:

    parse_vcf:               parses the VCF file and generates intermediate txt files
                             into the VCF_TEMP_DIR.

    load_parsed_vcf_data     loads the VCF data itself. This command requires the VCF file
                             to be parsed already. The resulting txt files are to be
                             stored in the VCF_TEMP_DIR specified in the params file.

    load_parsed_vcf_mapping  loads the VCF mapping, but not the data. This command requires
                             the VCF file to be parsed already. The resulting txt files are
                             to be stored in the VCF_TEMP_DIR specified in the params file.
                             This command only makes sense if the data is loaded as well.

    load_vcf:                parses the VCF file and loads the data and mapping into the
                             database.
                             = parse_vcf, load_parsed_vcf_data, load_parsed_vcf_mapping

    load_vcf_data:           parses the VCF file and only loads the VCF data itself. This
                             results in VCF data loaded in the deapp schema, but not mapped
                             to patients, and the data won't show up in the dataset
                             explorer tree.
                             = parse_vcf, load_parsed_vcf_data

    load_vcf_mapping         parses the VCF file and only loads the VCF mapping. This is
                             only useful if the VCF data itself is already loaded,
                             otherwise a mapping to subjects will be made, and a node in
                             the tree is created,  but the data itself doesn't exist.
                             = parse_vcf, load_parsed_vcf_mapping
