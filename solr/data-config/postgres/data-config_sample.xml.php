<dataConfig>
  <dataSource name="ds1" driver="org.postgresql.Driver"
              url="jdbc:postgresql://<?= $_ENV['PGHOST'] ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'] ?>"
              user="biomart_user" password="biomart_user" readOnly="true" />
  <document>
    <entity name="id" query="select sample_id,trial_name,disease,tissue_type,data_types,biobank,source_organism,sample_treatment,subject_treatment from i2b2DemoData.sample_categories">
      <field name="id" column="sample_id"/>
      <field name="DataSet" column="trial_name"/>
      <field name="Pathology" column="disease"/>
      <field name="Tissue" column="tissue_type"/>
      <field name="DataType" column="data_types"/>
      <field name="BioBank" column="bobank"/>
      <field name="Source_Organism" column="source_organism"/>
      <field name="Subject_Treatment" column="subject_treatment"/>
      <field name="Sample_Treatment" column="sample_treatment"/>
    </entity>
  </document>
</dataConfig>
