#!/usr/bin/perl

## i2b2 data were transferred from internal i2b2 SQL server already
## NGS data (VCF) were loaded to VCF-specific tables under schema DEAPP already

## This script will generate files to link VCF tables and existing i2b2 data and display on the Uer Interface

## This script has been adjusted to work for postgres databases

if ($#ARGV < 5) {
	print "Usage: perl convertMappingIntoSQLFiles.pl subject_sample_mapping_file output_dir study_id dataset_id fullpath(separated by +) dbname\n";
	print "    subject_sample_mapping_file is a tabseparated file with\n";
	print "        subject_id as known in clinical data in the first column\n";
	print "        sample_id as known in VCF file in the second column\n";
	print "    output_dir is a writable directory where the output files will be stored\n";
	print "    study_id is the study identifier used in clinical data\n";
	print "    dataset_id is the dataset identifier also used in loading the VCF data itself\n";
	print "    fullpath is the path in the dataset explorer tree where the VCF node should appear.\n";
	print "        The path must be delimited by + sign. Use quotation marks if the path contains whitespace\n";
	print "\n";
	print "Example: perl generate_VCF_mapping_files.pl subject_sample.txt /tmp/vcf GSE8581 GSE8581_Lung \"Public Studies+GSE8581+Exome Sequencing\" transmart\n";
	exit;
} else {
	our $subject_sample = $ARGV[0];
	our $output_dir = $ARGV[1];
	our $study_id = $ARGV[2];
	our $dataset_id = $ARGV[3];
	our $fullpath = $ARGV[4];
}

##### Do NOT modify stuff after this line *******

our ($subj_id, $sample_id, $hlevel, $path, $name, $path1, $attr, @fields);

## Loop through the full path and add proper data to affected tables
open CD, "> $output_dir/load_concept_dimension.sql" or die "Cannot open file: $!";
open IB, "> $output_dir/load_i2b2.sql" or die "Cannot open file: $!";

@fields = split (/\+/, $fullpath);

$path = "\\" . $fields[0] . "\\";
for ( $hlevel = 1; $hlevel <= $#fields; $hlevel++) {
	$name = $fields[$hlevel];
	$name =~ s/_/ /g;
	$path = $path . $name . "\\";

	# For postgres, the column 'table_name' doesn't exist. For that reason, it is removed from this SQL generation
	# Furthermore, the concept might already exist in the concept_dimension table. For that reason we use the
	# INSERT ... SELECT syntax (see http://stackoverflow.com/questions/4069718/postgres-insert-if-does-not-exist-already)

	if ($hlevel == 1) {
		$attr = "FA";
		$path1 = $path;
		print CD "insert into i2b2demodata.concept_dimension (concept_cd, concept_path, name_char, update_date, download_date, import_date, sourcesystem_cd)\n";
		print CD "   SELECT nextval( 'i2b2demodata.concept_id' ),'$path','$name',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,null \n";
		print CD "   WHERE NOT EXISTS ( SELECT concept_path FROM i2b2demodata.concept_dimension WHERE concept_path = '$path' );\n";
		
		print IB "insert into i2b2metadata.i2b2 (c_hlevel,c_fullname,c_name,c_synonym_cd,C_VISUALATTRIBUTES,C_BASECODE,C_FACTTABLECOLUMN,C_TABLENAME,\n";
		print IB "C_COLUMNNAME,C_COLUMNDATATYPE,C_OPERATOR,C_DIMCODE,C_COMMENT,C_TOOLTIP,UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,SOURCESYSTEM_CD,M_APPLIED_PATH)\n";
		print IB "   SELECT $hlevel,'$path','$name','N','$attr', concept_cd,\n";
		print IB "     'CONCEPT_CD','CONCEPT_DIMENSION','CONCEPT_PATH','T','LIKE','$path','trial:$study_id',\n";
		print IB "     '$path',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,null,'\@' \n";
		print IB "   FROM i2b2demodata.concept_dimension";
		print IB "   WHERE CONCEPT_PATH = '$path'";
		print IB "     AND NOT EXISTS( SELECT c_fullname FROM i2b2metadata.i2b2 WHERE c_fullname = '$path' );\n";
	} else {
		if ($hlevel == $#fields) {
			$attr = "LAH";
		} else {
			$attr = "FA";
		}
		print CD "insert into i2b2demodata.concept_dimension (concept_cd, concept_path, name_char, update_date, download_date, import_date, sourcesystem_cd)\n";
		print CD "   SELECT nextval( 'i2b2demodata.concept_id' ),'$path','$name',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'$dataset_id' \n";
		print CD "   WHERE NOT EXISTS ( SELECT concept_path FROM i2b2demodata.concept_dimension WHERE concept_path = '$path' );\n";

        print IB "insert into i2b2metadata.i2b2 (c_hlevel,c_fullname,c_name,c_synonym_cd,C_VISUALATTRIBUTES,C_BASECODE,C_FACTTABLECOLUMN,C_TABLENAME,\n";
        print IB "C_COLUMNNAME,C_COLUMNDATATYPE,C_OPERATOR,C_DIMCODE,C_COMMENT,C_TOOLTIP,UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,SOURCESYSTEM_CD,M_APPLIED_PATH)\n";
		print IB "   SELECT $hlevel,'$path','$name','N','$attr', concept_cd,\n";
		print IB "     'CONCEPT_CD','CONCEPT_DIMENSION','CONCEPT_PATH','T','LIKE','$path','trial:$study_id',\n";
		print IB "     '$path',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,'$dataset_id','\@'\n";
		print IB "   FROM i2b2demodata.concept_dimension";
		print IB "   WHERE CONCEPT_PATH = '$path'";
		print IB "     AND NOT EXISTS( SELECT c_fullname FROM i2b2metadata.i2b2 WHERE c_fullname = '$path' );\n";
	}

}
close CD;
close IB;

open MAPPING, "< $subject_sample" or die "Cannot open file: $!";
open DE, "> $output_dir/load_de_subject_sample_mapping.sql" or die "Cannot open file: $!";
open OF, "> $output_dir/load_observation_fact.sql" or die "Cannot open file: $!";
$subj_id = "";
$sample_id = "";

while (<MAPPING>) {
chomp;
        ($subj_id, $sample_id) = split (/\t/);
        if ($subj_id eq "" or $sample_id eq "") {
                die "The subject sample mapping file should be tab-delimited and have at least two columns.";
        }
        
        # Insert a record into the subject-sample-mapping table
        # The patient_num is retrieved from the i2b2demodata.patient_dimension table
        # The concept_code is retrieved from the i2b2demodata.concept_dimension table
        # The gpl_id is retrieved from the deapp.de_variant_dataset table
        print DE "insert into deapp.de_subject_sample_mapping (patient_id, subject_id, sample_cd, assay_id, concept_code, trial_name, platform, gpl_id)\n";
        print DE "   select patient_dimension.patient_num, '$subj_id', '$sample_id', nextval( 'deapp.seq_assay_id' ), concept_cd, '$dataset_id', 'VCF', gpl_id\n";
        print DE "      from i2b2demodata.concept_dimension, i2b2demodata.patient_dimension, deapp.de_variant_dataset\n";
        print DE "      where CONCEPT_PATH = '$path' AND patient_dimension.sourcesystem_cd='$study_id:$subj_id' AND dataset_id = '$dataset_id';\n";

		# Update the data in the summary table to have the proper assay_id. This is done after each subject_sample_mapping entry
		# in order to use the currval function, instead of looking up the assay_id afterwards.
		print DE "update deapp.de_variant_subject_summary SET assay_id = currval( 'deapp.seq_assay_id' ) WHERE dataset_id = '$dataset_id' AND subject_id = '$sample_id';\n\n"; 

		# Add an observation to the observation fact table
		print OF "insert into i2b2demodata.observation_fact (patient_num, concept_cd, provider_id, modifier_cd, valtype_cd,tval_char,valueflag_cd,location_cd,import_date,sourcesystem_cd,instance_num)\n";
		print OF "   select patient_dimension.patient_num, concept_cd,'\@','$dataset_id','T','$name','\@','\@',CURRENT_TIMESTAMP,'$dataset_id:$sample_id',1 from i2b2demodata.concept_dimension, i2b2demodata.patient_dimension  where CONCEPT_PATH = '$path'  AND patient_dimension.sourcesystem_cd='$study_id:$subj_id';\n";
}

close MAPPING;
close DE;
close OF;

open SECURE, "> $output_dir/load_i2b2_secure.sql" or die "Cannot open file: $!";

print SECURE " insert into i2b2metadata.i2b2_secure
	(c_hlevel,c_fullname,
	c_name,c_synonym_cd,
	C_VISUALATTRIBUTES,C_BASECODE,
	C_FACTTABLECOLUMN,C_TABLENAME,
	C_COLUMNNAME,C_COLUMNDATATYPE,
	C_OPERATOR,C_DIMCODE,
	C_COMMENT,C_TOOLTIP,
	UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,
	SOURCESYSTEM_CD,I2B2_ID,
	M_APPLIED_PATH,SECURE_OBJ_TOKEN)
 select 
	c_hlevel,c_fullname,
	c_name,c_synonym_cd,
	C_VISUALATTRIBUTES,C_BASECODE,
	C_FACTTABLECOLUMN,C_TABLENAME,
	C_COLUMNNAME,C_COLUMNDATATYPE,
	C_OPERATOR,C_DIMCODE,
	C_COMMENT,C_TOOLTIP,
	UPDATE_DATE,DOWNLOAD_DATE,IMPORT_DATE,
	SOURCESYSTEM_CD,null,
	M_APPLIED_PATH,'EXP:PUBLIC'
 from 
	i2b2metadata.i2b2 
 where 
	c_fullname like '$path1%';
\n";
close SECURE;

open COUNT, "> $output_dir/load_concept_counts.sql"  or die "Cannot open file: $!";

# The \ characters in the path must be escaped in the SQL stataement
my $escaped_path = $path1;
$escaped_path =~ s#\\#\\\\#g;

print COUNT " insert into i2b2demodata.concept_counts
      (concept_path
       ,parent_concept_path
        ,patient_count
       )
select
        fa.c_fullname,
        ltrim( 
          substr( 
            fa.c_fullname, 
            1, 
            char_length( fa.c_fullname ) - position( '\\' in substr( reverse( fa.c_fullname ), 2 ) ) 
          )
        ),
        count(distinct tpm.patient_num)
from
        i2b2metadata.i2b2 fa,
        i2b2demodata.observation_fact tpm,
        i2b2demodata.patient_dimension p
where
        fa.c_fullname like '$escaped_path%'
        and substr(fa.c_visualattributes,2,1) != 'H'
        and tpm.patient_num = p.patient_num
        and fa.c_basecode = tpm.concept_cd
       	-- Only add counts for new concepts 
        and NOT EXISTS(
        	select i.concept_path from i2b2demodata.concept_counts i where i.concept_path = fa.c_fullname
        )        
 group by
        fa.c_fullname,
        ltrim( 
          substr( 
            fa.c_fullname, 
            1, 
            char_length( fa.c_fullname ) - position( '\\' in substr( reverse( fa.c_fullname ), 2 ) ) 
          )
        )
        ;
\n";
close COUNT;
