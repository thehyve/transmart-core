<?php require __DIR__ . '/../../../lib/php/env_helper.inc.php'; ?>
<dataConfig>
  <dataSource driver="oracle.jdbc.driver.OracleDriver"
              url="jdbc:oracle:thin:@<?= $_ENV['ORAHOST'] ?><?= isset($_ENV['ORASVC']) ? "/{$_ENV['ORASVC']}" : ":{$_ENV['ORASID']}" ?>"
              user="biomart_user"
              password="<?= htmlspecialchars($biomart_user_pwd) ?>" />
  <document>
    <entity transformer="RegexTransformer" name="analysis" query="
select FACET_ID, fs.ANALYSIS_ID, STUDY, STUDY_ID, coalesce(SOLR_HIERARCHY,'') as SOLR_HIERARCHY, ANALYSES, DATA_TYPE, PLATFORM, PLATFORM_DESCRIPTION, PLATFORM_VENDOR, OBSERVATION, STUDY_TITLE,STUDY_DESCRIPTION,STUDY_DESIGN,STUDY_PRIMARY_INV,STUDY_CONTACT_FIELD,STUDY_OVERALL_DESIGN,STUDY_INSTITUTION,STUDY_ACCESSION,STUDY_COUNTRY,STUDY_BIOMARKER_TYPE,STUDY_TARGET,STUDY_ACCESS_TYPE,ANALYSIS_NAME,ANALYSIS_DESCRIPTION_S,ANALYSIS_DESCRIPTION_L,ANALYSIS_TYPE,ANALYSIS_ANALYST_NAME,ANALYSIS_METHOD,ANALYSIS_DATA_TYPE,ANALYSIS_QA_CRITERIA,MODEL_NAME,MODEL_DESCRIPTION,RESEARCH_UNIT from biomart.vw_faceted_search fs
LEFT JOIN biomart.vw_faceted_search_disease fsd ON fs.analysis_id = fsd.bio_assay_analysis_id
">
      <field name="FACET_ID" column="FACET_ID" />
      <field name="ANALYSIS_ID" column="ANALYSIS_ID" />
      <field name="STUDY" column="STUDY" />
      <field name="STUDY_ID" column="STUDY_ID" />
      <field name="ANALYSES" column="ANALYSES" />
      <field name="DATA_TYPE" column="DATA_TYPE" />
      <field name="PLATFORM" column="PLATFORM" />
      <field name="PLATFORM_DESCRIPTION" column="PLATFORM_VENDOR" />
      <field name="PLATFORM_VENDOR" column="PLATFORM_DESCRIPTION" />
      <field name="PLATFORM_NAME" column="PLATFORM_NAME" />
      <field name="OBSERVATION" column="OBSERVATION" />
      <field column="DISEASE" splitBy="/" sourceColName="SOLR_HIERARCHY" />

      <field name="STUDY_TITLE" column="STUDY_TITLE" />
      <field name="STUDY_DESCRIPTION" column="STUDY_DESCRIPTION" />
      <field name="STUDY_DESIGN" column="STUDY_DESIGN" />
      <field name="STUDY_PRIMARY_INV" column="STUDY_PRIMARY_INV" />
      <field name="STUDY_CONTACT_FIELD" column="STUDY_CONTACT_FIELD" />
      <field name="STUDY_OVERALL_DESIGN" column="STUDY_OVERALL_DESIGN" />
      <field name="STUDY_INSTITUTION" column="STUDY_INSTITUTION" />
      <field name="STUDY_ACCESSION" column="STUDY_ACCESSION" />
      <field name="STUDY_COUNTRY" column="STUDY_COUNTRY" />
      <field name="STUDY_BIOMARKER_TYPE" column="STUDY_BIOMARKER_TYPE" />
      <field name="STUDY_TARGET" column="STUDY_TARGET" />
      <field name="STUDY_ACCESS_TYPE" column="STUDY_ACCESS_TYPE" />
      <field name="ANALYSIS_NAME" column="ANALYSIS_NAME" />
      <field name="ANALYSIS_DESCRIPTION_S" column="ANALYSIS_DESCRIPTION_S" />
      <field name="ANALYSIS_DESCRIPTION_L" column="ANALYSIS_DESCRIPTION_L" />
      <field name="ANALYSIS_TYPE" column="ANALYSIS_TYPE" />
      <field name="ANALYSIS_ANALYST_NAME" column="ANALYSIS_ANALYST_NAME" />
      <field name="ANALYSIS_METHOD" column="ANALYSIS_METHOD" />
      <field name="ANALYSIS_DATA_TYPE" column="ANALYSIS_DATA_TYPE" />
      <field name="ANALYSIS_QA_CRITERIA" column="ANALYSIS_QA_CRITERIA" />
      <field name="MODEL_NAME" column="MODEL_NAME" />
      <field name="MODEL_DESCRIPTION" column="MODEL_DESCRIPTION" />
      <field name="RESEARCH_UNIT" column="RESEARCH_UNIT" splitBy="\|" />
    </entity>
  </document>
</dataConfig>

<!-- vim: et tw=80 ts=2 sw=2
-->
