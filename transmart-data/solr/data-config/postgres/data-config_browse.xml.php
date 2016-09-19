<?php require __DIR__ . '/../../../lib/php/env_helper.inc.php'; ?>
<dataConfig>
<dataSource name="ds1" driver="org.postgresql.Driver"
			url="jdbc:postgresql://<?= $host ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'] ?>"
			user="biomart_user" password="<?= htmlspecialchars($biomart_user_pwd) ?>"
			readOnly="true" autoCommit="false" />
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
        <field name="id" column="id" />
        <field name="TYPE" column="type" />
        <field name="SUBTYPE" column="subtype" />
        <field name="title" column="title"/>
        <field name="description" column="description"/>
        <field name="DISEASE" column="disease" splitBy="\|"/>
        <field name="OBSERVATION" column="observation" splitBy="\|" />
        <field name="PATHWAY" column="pathway" splitBy="\|" />
        <field name="GENE" column="gene" splitBy="\|" />
        <field name="THERAPEUTIC_DOMAIN" column="therapeutic_domain" splitBy="\|" />
        <field name="PROGRAM_INSTITUTION" column="institution" splitBy="\|" />
        <field name="PROGRAM_TARGET" column="target" splitBy="\|" />
        <field name="ACCESSION" column="id" />
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
        <field name="id" column="id" />
        <field name="TYPE" column="type" />
        <field name="SUBTYPE" column="subtype" />
        <field name="title" column="title"/>
        <field name="description" column="description"/>
        <field name="STUDY_DESIGN" column="design" splitBy="\|" />
        <field name="STUDY_BIOMARKER_TYPE" column="biomarker_type" splitBy="\|" />
        <field name="STUDY_ACCESS_TYPE" column="access_type" splitBy="\|" />
        <field name="ACCESSION" column="accession" />
        <field name="STUDY_INSTITUTION" column="institution" splitBy="\|" />
        <field name="COUNTRY" column="country" splitBy="\|" />
        <field name="DISEASE" column="disease" splitBy="\|" />
        <field name="COMPOUND" column="compound" splitBy="\|" />
        <field name="STUDY_OBJECTIVE" column="study_objective" splitBy="\|" />
        <field name="SPECIES" column="organism" splitBy="\|" />
        <field name="STUDY_PHASE" column="study_phase" splitBy="\|" />
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
            <field name="id" column="id" />
            <field name="TYPE" column="type" />
            <field name="SUBTYPE" column="subtype" />
            <field name="title" column="title"/>
            <field name="description" column="description"/>
            <field name="ASSAY_MEASUREMENT_TYPE" column="measurement_type" splitBy="\|" />
            <field name="GENE" column="gene" splitBy="\|" />
            <field name="MIRNA" column="mirna" splitBy="\|" />
            <field name="ASSAY_TYPE_OF_BM_STUDIED" column="biomarker_type" splitBy="\|" />
            <field name="ASSAY_MEASUREMENT_TYPE" column="measurement_type" splitBy="\|" />
            <field name="ASSAY_PLATFORM_NAME" column="platform_name" splitBy="\|" />
            <field name="ASSAY_VENDOR" column="vendor" splitBy="\|" />
            <field name="ASSAY_TECHNOLOGY" column="technology" splitBy="\|" />
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
        <field name="id" column="id" />
        <field name="TYPE" column="type" />
        <field name="SUBTYPE" column="subtype" />
        <field name="title" column="title"/>
        <field name="description" column="description"/>
        <field name="ANALYSIS_MEASUREMENT_TYPE" column="measurement_type" splitBy="\|" />
        <field name="ANALYSIS_PLATFORM_NAME" column="platform_name" splitBy="\|" />
        <field name="ANALYSIS_VENDOR" column="vendor" splitBy="\|" />
        <field name="ANALYSIS_TECHNOLOGY" column="technology" splitBy="\|" />
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
        <field name="id" column="id" />
        <field name="TYPE" column="type" />
        <field name="SUBTYPE" column="subtype" />
        <field name="title" column="title" />
        <field name="description" column="description" />
        <field name="FILE_TYPE" column="file_type" splitBy="\|"/>
    </entity>
    </document>
</dataConfig>
