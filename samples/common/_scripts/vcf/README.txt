Parse and Load VCF data to tranSMART.

These scripts are a modified version of the original oracle loading scripts, found at
https://github.com/transmart/transmartApp-DB/tree/master/VCF_db_scripts, adjusted
to also work with postgres databases.

Prerequisites:
1. Make sure Perl is installed and working on your machine. The scripts expects a command like
   $ perl script.pl
   to run correctly, so perl must exist on your path.

In order to load your VCF data, follow these steps:

1. Create patient subject-sample mapping file. This file should have two columns (tab delimited). 
   Column 1 is the identification of the patient (the SUBJ_ID for the study), and column 2 is the sample 
   ID from the VCF file. By default, this file should have the name <subject_sample_mapping.txt>
   although you may change the name, as long as the changes are reflected in <generate_VCF_mapping_files.pl>
   (see step 4).
   Note: this file is not provided, as it is specific to the VCF data.

2. Alter the parameters in the vcf.params file

3. Source the params file:
	. ./vcf.params

4. Source the transmart-data vars file:
   . ./vars

5. Run make samples/{oracle,postgres} load_vcf (from the transmart-data root directory)
