<?php require __DIR__ . '/../../../lib/php/env_helper.inc.php'; ?>
<dataConfig>
<dataSource driver="oracle.jdbc.driver.OracleDriver"
            url="jdbc:oracle:thin:@<?= $_ENV['ORAHOST'] ?>:<?= $_ENV['ORAPORT'] ?><?= isset($_ENV['ORASVC']) ? "/{$_ENV['ORASVC']}" : ":{$_ENV['ORASID']}" ?>"
            user="biomart_user" password="<?= htmlspecialchars($biomart_user_pwd) ?>">
    <document>
        <entity name="I2B2" query="select SAMPLE_ID,TRIAL_NAME,DISEASE,TISSUE_TYPE,DATA_TYPES,BIOBANK,SOURCE_ORGANISM,SAMPLE_TREATMENT,SUBJECT_TREATMENT from i2b2DemoData.sample_categories">
            <field name="id" column="SAMPLE_ID" />
            <field name="DataSet" column="TRIAL_NAME" />
            <field name="Pathology" column="DISEASE" />
            <field name="Tissue" column="TISSUE_TYPE" />
            <field name="DataType" column="DATA_TYPES" />
        <field name="BioBank" column="BIOBANK" />
            <field name="Source_Organism" column="SOURCE_ORGANISM" />
            <field name="Subject_Treatment" column="SUBJECT_TREATMENT" />
        <field name="Sample_Treatment" column="SAMPLE_TREATMENT" />
        </entity>

    <entity name="PROGRAM" transformer="RegexTransformer" query="
select 
  id
, 'FOLDER' as type
, 'PROGRAM' as subtype
, title
, description
, disease
, observation
, pathway
, gene
, therapeutic_domain
, institution
, target
from biomart_user.browse_programs_view
where ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">
        <field name="id" column="ID" />
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
	<field name="title" column="TITLE"/>
        <field name="description" column="DESCRIPTION"/>
        <field name="DISEASE" column="DISEASE" splitBy="\|"/>
        <field name="OBSERVATION" column="OBSERVATION" splitBy="\|" />
        <field name="PATHWAY" column="PATHWAY" splitBy="\|" />
        <field name="GENE" column="GENE" splitBy="\|" />
        <field name="THERAPEUTIC_DOMAIN" column="THERAPEUTIC_DOMAIN" splitBy="\|" />
        <field name="PROGRAM_INSTITUTION" column="INSTITUTION" splitBy="\|" />
        <field name="PROGRAM_TARGET" column="TARGET" splitBy="\|" />
        <field name="ACCESSION" column="ID" />
    </entity>

    <entity name="STUDY" transformer="RegexTransformer" query="
select
  id
, 'FOLDER' as type
, 'STUDY' as subtype
, title
, description
, design
, biomarker_type
, access_type
, accession
, institution
, country
, disease
, compound
, study_objective
, organism
, study_phase
from biomart_user.browse_studies_view
where ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">
        <field name="id" column="ID" />
	<field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="title" column="TITLE"/>
        <field name="description" column="DESCRIPTION"/>
        <field name="STUDY_DESIGN" column="DESIGN" splitBy="\|" />
	<field name="STUDY_BIOMARKER_TYPE" column="BIOMARKER_TYPE" splitBy="\|" />
        <field name="STUDY_ACCESS_TYPE" column="ACCESS_TYPE" splitBy="\|" />
        <field name="ACCESSION" column="ACCESSION" />
        <field name="STUDY_INSTITUTION" column="INSTITUTION" splitBy="\|" />
        <field name="COUNTRY" column="COUNTRY" splitBy="\|" />
	<field name="DISEASE" column="DISEASE" splitBy="\|" />
        <field name="COMPOUND" column="COMPOUND" splitBy="\|" />
        <field name="STUDY_OBJECTIVE" column="STUDY_OBJECTIVE" splitBy="\|" />
	<field name="SPECIES" column="ORGANISM" splitBy="\|" />
        <field name="STUDY_PHASE" column="STUDY_PHASE" splitBy="\|" />
    </entity>


    <entity name="ASSAY" transformer="RegexTransformer" query="
    select
      id
    , 'FOLDER' as type
    , 'ASSAY' as subtype
    , title
    , description
    , measurement_type
    , platform_name
    , vendor
    , technology
    , gene
    , mirna
    , biomarker_type
    from biomart_user.browse_assays_view
    where ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
    ">
            <field name="id" column="ID" />
            <field name="TYPE" column="TYPE" />
            <field name="SUBTYPE" column="SUBTYPE" />
            <field name="title" column="TITLE"/>
            <field name="description" column="DESCRIPTION"/>
            <field name="ASSAY_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
            <field name="GENE" column="GENE" splitBy="\|" />
            <field name="MIRNA" column="MIRNA" splitBy="\|" />
            <field name="ASSAY_TYPE_OF_BM_STUDIED" column="BIOMARKER_TYPE" splitBy="\|" />
            <field name="ASSAY_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
            <field name="ASSAY_PLATFORM_NAME" column="PLATFORM_NAME" splitBy="\|" />
            <field name="ASSAY_VENDOR" column="VENDOR" splitBy="\|" />
            <field name="ASSAY_TECHNOLOGY" column="TECHNOLOGY" splitBy="\|" />
        </entity>
    <entity name="ANALYSIS" transformer="RegexTransformer" query="
select
  id
, 'FOLDER' as type
, 'ANALYSIS' as subtype
, title
, description
, measurement_type
, platform_name
, vendor
, technology
from biomart_user.browse_analyses_view
where ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">
        <field name="id" column="ID" />
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="title" column="TITLE"/>
        <field name="description" column="DESCRIPTION"/>
        <field name="ANALYSIS_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
        <field name="ANALYSIS_PLATFORM_NAME" column="PLATFORM_NAME" splitBy="\|" />
        <field name="ANALYSIS_VENDOR" column="VENDOR" splitBy="\|" />
        <field name="ANALYSIS_TECHNOLOGY" column="TECHNOLOGY" splitBy="\|" />
    </entity>

    <entity name="FOLDER" transformer="RegexTransformer" query="
select 
  id
, 'FOLDER' as type
, 'FOLDER' as subtype
, title
, description
, file_type
from biomart_user.browse_folders_view
where ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">
        <field name="id" column="ID" />
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="title" column="TITLE" />
        <field name="description" column="DESCRIPTION" />
        <field name="FILE_TYPE" column="FILE_TYPE" splitBy="\|"/>
    </entity>
    </document>
</dataConfig>
