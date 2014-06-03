Parse and Load VCF data to tranSMART.

These scripts are a modified version of the original oracle loading scripts, found at
https://github.com/transmart/transmartApp-DB/tree/master/VCF_db_scripts, adjusted
to work with postgres databases. 

Currently, the scripts assume that your are able to run psql and access the transmart 
database. As for now, there is no username or password used in the scripts.

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

2. Run the script <load_vcf_data.sh> to load the VCF data itself into transmart. 
   (Use chmod +x if necessary)
   This script requires 7 parameters:
   		vcf_input_file		Name of the input VCF file 
   		datasource 			Datasource ID, a textual description of the source of the data
   		dataset_id 			Dataset ID, a unique identifier for this dataset
		gpl_id 				An identifier for the platform to use.A platform for VCF currently 
						only describes the genome build. If unsure, use 'VCF_<genome_build>'
		genome_build   			Genome build used as a reference
   		ETL_user 			Name or initials of the ETL user
   		dbname 				Database name for transmart data 
   
   $ ./load_vcf_data.sh ....
   
   This command runs several scripts:
       generate_VCF_loading_files.pl 
       		which will generate 5 text files to be loaded later on. This script
       		is exactly the same as the oracle version.
       convert_VCF_loading_files_to_postgres.pl 
       		which converts two of the files, so that they can be loaded easily 
       		into a postgres database.
       create_postgres_loading_scripts.pl
       		which creates a shell script to load all data files into postgres. This
       		script will also add the absolute path name to the postgres scripts,
       		as the postgres COPY command requires absolute paths.
	   load_VCF_postgres.sh
	   	which runs the actual postgres commands (psql) to load the data.

3. Run the script <load_mapping_data.sh> to map the VCF data to the patients in i2b2.
   (Use chmod +x if necessary)
   
   $ ./load_mapping_data.sh
   This script requires 5 parameters:
		subject_sample_mapping_file		Name of the subject-sample mapping file, as generated in step 1 
		study_id 				Study ID that was used to load clinical data. This is used to lookup
							the proper subjects, based on their subject ID.
		dataset_id 				Dataset ID, the identifier for this dataset, as also used in step 2
		fullpath(separated by +) 		Full path of the concept to store the VCF data
		dbname   				Database name for transmart data

   This command runs several scripts:
       generate_VCF_mapping_files.pl
       		which will generate 6 sql files to be loaded later on and the
       		<load_mapping_tables.sh> script to load the data. 
	   load_mapping_tables.sh
	   		which runs the actual postgres commands (psql) to load the data.
