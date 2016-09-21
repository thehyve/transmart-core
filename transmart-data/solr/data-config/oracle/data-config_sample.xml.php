<?php require __DIR__ . '/../../../lib/php/env_helper.inc.php'; ?>
<dataConfig>
<dataSource driver="oracle.jdbc.driver.OracleDriver"
            url="jdbc:oracle:thin:@<?= $_ENV['ORAHOST'] ?>:<?= $_ENV['ORAPORT'] ?><?= isset($_ENV['ORASVC']) ? "/{$_ENV['ORASVC']}" : ":{$_ENV['ORASID']}" ?>"
            user="biomart_user" password="<?= htmlspecialchars($biomart_user_pwd) ?>" />
    <document>
        <entity name="Samples" query="select SAMPLE_ID,TRIAL_NAME,FIELD1,FIELD2,FIELD3,FIELD4,FIELD5,FIELD6,FIELD7,FIELD8,FIELD9,FIELD10,SOURCE_ORGANISM from i2b2DemoData.sample_categories">
            <field name="id" column="SAMPLE_ID"/>
            <field name="trial_name" column="TRIAL_NAME"/>
            <field name="barcode" column="FIELD1"/>
            <field name="plate_id" column="FIELD2"/>
            <field name="patient_id" column="FIELD3"/>
            <field name="external_id" column="FIELD4"/>
            <field name="aliquot_id" column="FIELD5"/>
            <field name="visit" column="FIELD6"/>
            <field name="sample_type" column="FIELD7"/>
            <field name="description" column="FIELD8"/>
            <field name="comment" column="FIELD9"/>
            <field name="location" column="FIELD10"/>
            <field name="source_organism" column="SOURCE_ORGANISM"/>
        </entity>
    </document>
</dataConfig>
