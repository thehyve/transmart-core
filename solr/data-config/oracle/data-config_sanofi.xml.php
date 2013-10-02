<dataConfig>
<dataSource driver="oracle.jdbc.driver.OracleDriver" url="jdbc:oracle:thin:@<?= $_ENV['ORAHOST'] ?>:<?= $_ENV['ORAPORT'] ?>:<?= $_ENV['ORASID'] ?>" user="biomart_user" password="biomart_user" />
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
select fd.unique_id as id
, 'FOLDER' as type
, 'PROGRAM' as subtype
, f.folder_name as title
, f.description as description
, x.disease
, x.observation
, x.pathway
, x.gene
, x.therapeutic_domain
, x.institution
, x.target
from fmapp.fm_folder f
inner join fmapp.fm_data_uid fd on f.folder_id = fd.fm_data_id
left outer join
  (select id, disease, observation, pathway, gene, therapeutic_domain, institution, target  from
    (
    select
      fdu.unique_id as id, 'BIO_DISEASE' as object_type, ata.object_uid as object_uid
      from
        fmapp.fm_folder ff
        inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
        inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
        inner join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
        inner join biomart.bio_disease bd on bd.bio_disease_id = bdu.bio_data_id

      where
        ata.object_type in ('BIO_DISEASE', 'PROGRAM_TARGET')
        and ff.folder_type = 'PROGRAM'

    union

    select
      fdu.unique_id as id, 'BIO_OBSERVATION' as object_type, ata.object_uid as object_uid
      from
        fmapp.fm_folder ff
        inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
        inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
        inner join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
        inner join biomart.bio_observation bo on bo.bio_observation_id = bdu.bio_data_id

      where
        ata.object_type in ('BIO_OBSERVATION', 'PROGRAM_TARGET')
        and ff.folder_type = 'PROGRAM'

    union

        select
      fdu.unique_id as id, 'PATHWAY' as object_type, ata.object_uid as object_uid
      from
        fmapp.fm_folder ff
        inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
        inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
      inner join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
      inner join biomart.bio_marker bm on bm.bio_marker_id = bdu.bio_data_id

      where
        bm.bio_marker_type = 'PATHWAY'
        and (ata.object_type = 'BIO_MARKER' or ata.object_type = 'PROGRAM_TARGET')
        and ff.folder_type = 'PROGRAM'

    union

        select
      fdu.unique_id as id, 'GENE' as object_type, ata.object_uid as object_uid
      from
        fmapp.fm_folder ff
        inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
        inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
        inner join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
        inner join biomart.bio_marker bm on bm.bio_marker_id = bdu.bio_data_id

      where
        bm.bio_marker_type = 'GENE'
        and (ata.object_type = 'BIO_MARKER' or ata.object_type = 'PROGRAM_TARGET')
        and ff.folder_type = 'PROGRAM'

    union

    select
      fdu.unique_id as id, to_char(ati.code_type_name) as object_type, ata.object_uid as object_uid
    from
      fmapp.fm_folder ff
      inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
      inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
      inner join amapp.am_tag_item ati on ata.tag_item_id = ati.tag_item_id
      inner join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
      inner join biomart.bio_concept_code bcc on bcc.bio_concept_code_id = bdu.bio_data_id
    where
      ata.object_type in ('BIO_CONCEPT_CODE', 'PROGRAM_TARGET')
      and ff.folder_type = 'PROGRAM'

  ) pivot (
    listagg(to_char(object_uid), '|') within group (order by object_uid)
    for object_type in ('BIO_DISEASE' as disease, 'BIO_OBSERVATION' as observation, 'PATHWAY' as pathway, 'GENE' as gene, 'THERAPEUTIC_DOMAIN' as therapeutic_domain, 'PROGRAM_INSTITUTION' as institution, 'PROGRAM_TARGET_PATHWAY_PHENOTYPE' as target)
  )
  ) x on x.id = fd.unique_id
where f.folder_type = 'PROGRAM' and f.active_ind = 1
and ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">

        <field name="id" column="ID" />
        <field name="title" column="TITLE"/>
        <field name="description" column="DESCRIPTION"/>
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="ANALYSIS_ID" column="ANALYSIS_ID" />
        <field name="ANALYSIS_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
        <field name="DISEASE" column="DISEASE" splitBy="\|"/>
        <field name="GENE" column="GENE" splitBy="\|" />
        <field name="OBSERVATION" column="OBSERVATION" splitBy="\|" />
        <field name="PATHWAY" column="PATHWAY" splitBy="\|" />
        <field name="THERAPEUTIC_DOMAIN" column="THERAPEUTIC_DOMAIN" splitBy="\|" />
        <field name="PROGRAM_INSTITUTION" column="INSTITUTION" splitBy="\|" />
        <field name="PROGRAM_TARGET" column="TARGET" splitBy="\|" />
        <field name="ACCESSION" column="ID" />
    </entity>

    <entity name="STUDY" transformer="RegexTransformer" query="
SELECT
exp.title as title
, exp.description as description
, exp.design as design
, exp.biomarker_type as biomarker_type
, exp.access_type as access_type
, exp.accession as accession
, exp.institution as institution
, exp.country as country
, fd.unique_id as id
, 'FOLDER' as type
, 'STUDY' as subtype
, x.disease as disease
, x.compound as compound
, x.study_objective as study_objective
, x.species as organism
, x.phase as study_phase
from biomart.bio_experiment exp
inner join biomart.bio_data_uid bd on exp.bio_experiment_id = bd.bio_data_id
inner join fmapp.fm_folder_association fa on fa.object_uid = bd.unique_id
inner join fmapp.fm_data_uid fd on fa.folder_id = fd.fm_data_id
inner join fmapp.fm_folder ff on ff.folder_id = fa.folder_id
left outer join (select id, disease, compound, study_objective, species, phase from
  (
  select
    fdu.unique_id as id, ata.object_type as object_type, ata.object_uid as object_uid
  from
    fmapp.fm_folder_association ffa
    inner join fmapp.fm_data_uid fdu on ffa.folder_id = fdu.fm_data_id
    inner join amapp.am_tag_association ata on fdu.unique_id= ata.subject_uid

  where
    ata.object_type in ('BIO_DISEASE', 'BIO_COMPOUND')

  union
  select
    fdu.unique_id as id, ati.code_type_name as object_type, ata.object_uid as object_uid
  from
    fmapp.fm_folder_association ffa
    inner join fmapp.fm_data_uid fdu on ffa.folder_id = fdu.fm_data_id
    inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
    inner join amapp.am_tag_item ati on ata.tag_item_id = ati.tag_item_id
  where
    ata.object_type = 'BIO_CONCEPT_CODE'
) pivot (
  listagg(TO_CHAR(object_uid), '|') within group (order by object_uid)
  for object_type in ('BIO_DISEASE' as disease, 'BIO_COMPOUND' as compound, 'STUDY_OBJECTIVE' as study_objective, 'SPECIES' as species, 'STUDY_PHASE' as phase)
)
)x on  x.id = fd.unique_id
where ff.active_ind = 1
and ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
">
        <field name="id" column="ID" />
                <field name="title" column="TITLE"/>
                <field name="description" column="DESCRIPTION"/>
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="DISEASE" column="DISEASE" splitBy="\|" />
        <field name="COMPOUND" column="COMPOUND" splitBy="\|" />
        <field name="STUDY_OBJECTIVE" column="STUDY_OBJECTIVE" splitBy="\|" />
        <field name="STUDY_PHASE" column="STUDY_PHASE" splitBy="\|" />
        <field name="SPECIES" column="ORGANISM" splitBy="\|" />
        <field name="STUDY_ID" column="STUDY_ID" />
        <field name="STUDY_DESIGN" column="DESIGN" splitBy="\|" />
        <field name="STUDY_BIOMARKER_TYPE" column="BIOMARKER_TYPE" splitBy="\|" />
        <field name="STUDY_ACCESS_TYPE" column="ACCESS_TYPE" splitBy="\|" />
        <field name="STUDY_INSTITUTION" column="INSTITUTION" splitBy="\|" />
        <field name="COUNTRY" column="COUNTRY" splitBy="\|" />
        <field name="ACCESSION" column="ACCESSION" />
    </entity>

    <entity name="ASSAY" transformer="RegexTransformer" query="
select DISTINCT fd.unique_id as id
, 'FOLDER' as type
, 'ASSAY' as subtype
, f.folder_name as title
, f.description as description
, listagg(to_char(bdu.unique_id), '|') within group (order by bdu.unique_id) as bio_assay_platform
, listagg(to_char(bap.platform_type), '|') within group (order by bap.platform_type) as measurement_type
, listagg(to_char(bap.platform_name), '|') within group (order by bap.platform_name ) as platform_name
, listagg(to_char(bap.platform_vendor), '|') within group (order by bap.platform_vendor) as vendor
, listagg(to_char(bap.platform_technology), '|') within group (order by bap.platform_technology) as technology
, x.gene
, x.biomarker_type
from fmapp.fm_folder f
inner join fmapp.fm_data_uid fd on f.folder_id = fd.fm_data_id
left outer join amapp.am_tag_association ata on fd.unique_id = ata.subject_uid and ata.object_type = 'BIO_ASSAY_PLATFORM'
left outer join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
left outer join biomart.bio_assay_platform bap on bap.bio_assay_platform_id = bdu.bio_data_id
left outer join
  (select id, gene, biomarker_type from
    (
    select
      fdu.unique_id as id, ata.object_type as object_type, ata.object_uid as object_uid
      from
        fmapp.fm_folder ff
        inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
        inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid

      where
        ata.object_type in ('BIO_MARKER')
        and ff.folder_type = 'ASSAY'

    union
    select
      fdu.unique_id as id, ati.code_type_name as object_type, ata.object_uid as object_uid
    from
      fmapp.fm_folder ff
      inner join fmapp.fm_data_uid fdu on ff.folder_id = fdu.fm_data_id
      inner join amapp.am_tag_association ata on fdu.unique_id = ata.subject_uid
      inner join amapp.am_tag_item ati on ata.tag_item_id = ati.tag_item_id
    where
      ata.object_type = 'BIO_CONCEPT_CODE'
      and ff.folder_type = 'ASSAY'
  ) pivot (
    listagg(to_char(object_uid), '|') within group (order by object_uid)
    for object_type in ('BIO_MARKER' as gene,'ASSAY_TYPE_OF_BM_STUDIED' as biomarker_type)
  )
  )x on x.id = fd.unique_id
where f.folder_type = 'ASSAY' and f.active_ind = 1
and ('${dataimporter.request.clean}' != 'false' or id = '${dataimporter.request.uid}')
group by fd.unique_id, f.folder_name, f.description, x.gene,  x.biomarker_type
">
        <field name="id" column="ID" />
                <field name="title" column="TITLE"/>
                <field name="description" column="DESCRIPTION"/>
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="GENE" column="GENE" splitBy="\|" />
        <field name="ASSAY_TYPE_OF_BM_STUDIED" column="BIOMARKER_TYPE" splitBy="\|" />
        <field name="ASSAY_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
        <field name="ASSAY_PLATFORM_NAME" column="PLATFORM_NAME" splitBy="\|" />
        <field name="ASSAY_VENDOR" column="VENDOR" splitBy="\|" />
        <field name="ASSAY_TECHNOLOGY" column="TECHNOLOGY" splitBy="\|" />
    </entity>

    <entity name="ANALYSIS" transformer="RegexTransformer" query="
select
fd.unique_id as id
, 'FOLDER' as type
, 'ANALYSIS' as subtype
, baa.analysis_name as title
, baa.long_description as description
, listagg(to_char(bdu.unique_id), '|') within group (order by bdu.unique_id) as bio_assay_platform
, listagg(to_char(bap.platform_type), '|') within group (order by bap.platform_type) as measurement_type
, listagg(to_char(bap.platform_name), '|') within group (order by bap.platform_name ) as platform_name
, listagg(to_char(bap.platform_vendor), '|') within group (order by bap.platform_vendor) as vendor
, listagg(to_char(bap.platform_technology), '|') within group (order by bap.platform_technology) as technology
from biomart.bio_assay_analysis baa
inner join biomart.bio_data_uid bd on baa.bio_assay_analysis_id = bd.bio_data_id
inner join fmapp.fm_folder_association fa on fa.object_uid = bd.unique_id
inner join fmapp.fm_data_uid fd on fa.folder_id = fd.fm_data_id
inner join fmapp.fm_folder ff on ff.folder_id = fa.folder_id
left outer join amapp.am_tag_association ata on fd.unique_id = ata.subject_uid
left outer join biomart.bio_data_uid bdu on bdu.unique_id = ata.object_uid
left outer join biomart.bio_assay_platform bap on bap.bio_assay_platform_id = bdu.bio_data_id
where
    ata.object_type in ('BIO_ASSAY_PLATFORM')
    and ff.active_ind = 1
    and ('${dataimporter.request.clean}' != 'false' or fd.unique_id = '${dataimporter.request.uid}')
group by  fd.unique_id, baa.analysis_name, baa.long_description
">
        <field name="id" column="ID" />
                <field name="title" column="TITLE"/>
                <field name="description" column="DESCRIPTION"/>
        <field name="TYPE" column="TYPE" />
        <field name="SUBTYPE" column="SUBTYPE" />
        <field name="ANALYSIS_ID" column="ANALYSIS_ID" />
        <field name="ANALYSIS_MEASUREMENT_TYPE" column="MEASUREMENT_TYPE" splitBy="\|" />
        <field name="ANALYSIS_PLATFORM_NAME" column="PLATFORM_NAME" splitBy="\|" />
        <field name="ANALYSIS_VENDOR" column="VENDOR" splitBy="\|" />
        <field name="ANALYSIS_TECHNOLOGY" column="TECHNOLOGY" splitBy="\|" />
    </entity>

    <entity name="FOLDER" transformer="RegexTransformer" query="
select fd.unique_id as id
, 'FOLDER' as type
, 'FOLDER' as subtype
, f.folder_name as title
, f.description as description
, listagg(to_char(ata.object_uid), '|') within group (order by ata.object_uid) as file_type
from fmapp.fm_folder f
inner join fmapp.fm_data_uid fd on f.folder_id = fd.fm_data_id
left outer join amapp.am_tag_association ata on fd.unique_id = ata.subject_uid
where f.folder_type = 'FOLDER' and f.active_ind = 1
and ata.object_type='BIO_CONCEPT_CODE'
and ata.object_uid like 'FILE_TYPE%'
and ('${dataimporter.request.clean}' != 'false' or fd.unique_id = '${dataimporter.request.uid}')
group by fd.unique_id, f.folder_name, f.description
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
