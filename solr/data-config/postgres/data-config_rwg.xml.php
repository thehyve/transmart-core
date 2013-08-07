<dataConfig>
<dataSource name="ds1" driver="org.postgresql.Driver"
			url="jdbc:postgresql://<?= $_ENV['PGHOST'] ?>:<?= $_ENV['PGPORT'] ?>/<?= $_ENV['PGDATABASE'] ?>"
			user="biomart_user" password="biomart_user" readOnly="true"
 autoCommit="false" />

    <document name="terms">

        <entity name="analysis"
		query="select bio_assay_analysis_id, FOLD_CHANGE_CUTOFF, PVALUE_CUTOFF  from biomart.bio_assay_analysis b1 where exists (select 1 from biomart.bio_analysis_attribute b2 where b1.bio_assay_analysis_id=b2.bio_assay_analysis_id) "
		deltaImportQuery="select bio_assay_analysis_id, FOLD_CHANGE_CUTOFF, PVALUE_CUTOFF  from biomart.bio_assay_analysis b1 where b1.bio_assay_analysis_id='${dataimporter.delta.bio_assay_analysis_id}'"

		deltaQuery="select bio_assay_analysis_id from biomart.bio_assay_analysis b1 where exists (select 1 from biomart.bio_analysis_attribute b2 where b1.bio_assay_analysis_id=b2.bio_assay_analysis_id)
		and (analysis_create_date &gt; to_date('${dataimporter.last_index_time}', 'yyyy-mm-dd hh24:mi:ss')
		     OR analysis_update_date &gt; to_date('${dataimporter.last_index_time}', 'yyyy-mm-dd hh24:mi:ss'))"

>
            <field name="ANALYSIS_ID" column="bio_assay_analysis_id" />

			<!-- this one is for study keyword -->
            <entity name="study" query="SELECT distinct sk.search_keyword_id SEARCH_KEYWORD_ID FROM biomart.bio_analysis_attribute baa, search_keyword sk
                           WHERE baa.study_id  = sk.keyword AND bio_assay_analysis_id = '${analysis.bio_assay_analysis_id}'">
				<field name="STUDY" column="search_keyword_id" />
            </entity>

			<!-- also retrieve the study ID (could be done by adding a field above, but not until keywords linked in data load -->
            <entity name="studyid" query="select distinct study_id from bio_analysis_attribute WHERE bio_assay_analysis_id = '${analysis.bio_assay_analysis_id}'">
				<field name="STUDY_ID" column="study_id" />
            </entity>

            <entity name="ta" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='THERAPEUTIC AREAS'">
				<field name="THERAPEUTIC_AREAS" column="search_keyword_id" />
            </entity>

            <entity name="analyses" query="SELECT ancestor_search_keyword_id AS search_keyword_id FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='ANALYSES'">
				<field name="ANALYSES" column="search_keyword_id" />
            </entity>

            <entity name="datatype" query="SELECT ancestor_search_keyword_id AS search_keyword_id FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='DATA TYPE'">
				<field name="DATA_TYPE" column="search_keyword_id" />
            </entity>

            <entity name="experimentaldesign" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='EXPERIMENTAL DESIGN'">
				<field name="EXPERIMENTAL_DESIGN" column="search_keyword_id" />
            </entity>

            <entity name="sampletype" query="SELECT ancestor_search_keyword_id search_keyword_id FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='SAMPLE TYPE'">
				<field name="SAMPLE_TYPE" column="search_keyword_id" />
            </entity>

            <entity name="treatment" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='TREATMENT'">
				<field name="TREATMENT" column="search_keyword_id" />
            </entity>

            <entity name="organism" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='ORGANISM'">
				<field name="ORGANISM" column="search_keyword_id" />
            </entity>


            <entity name="datasource" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
                                                   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
                                                   AND sk.data_category='DATA_SOURCE'">
                                <field name="DATA_SOURCE" column="search_keyword_id" />
            </entity>

            <entity name="platform" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='PLATFORM'">
				<field name="PLATFORM" column="search_keyword_id" />
            </entity>

            <entity name="pathology" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='PATHOLOGY'">
				<field name="PATHOLOGY" column="search_keyword_id" />
            </entity>

            <entity name="trial" query="select distinct search_keyword_id from search_keyword s,  bio_assay_analysis_data b where s.bio_data_id = b.bio_experiment_id
                                        AND bio_assay_analysis_id='${analysis.bio_assay_analysis_id}' AND data_category = 'TRIAL'">
				<field name="TRIAL" column="search_keyword_id" />
            </entity>

            <entity name="compound" query="SELECT distinct search_keyword_id
                           FROM bio_experiment be, bio_compound bc, bio_data_compound bdc, search_keyword sk, bio_assay_analysis_data baad
                           WHERE bdc.bio_data_id = be.bio_experiment_id
                           AND bdc.bio_compound_id = bc.bio_compound_id
                           AND bc.bio_compound_id=sk.bio_data_id
                           AND sk.data_category='COMPOUND'
                           AND be.bio_experiment_id = baad.bio_experiment_id
                           AND bio_assay_analysis_id = ${analysis.bio_assay_analysis_id}">
				<field name="COMPOUND" column="search_keyword_id" />
            </entity>

            <entity name="disease" query="SELECT ancestor_search_keyword_id SEARCH_KEYWORD_ID FROM bio_analysis_attribute_lineage baal, bio_analysis_attribute baa, search_keyword sk
                           WHERE baal.bio_analysis_attribute_id=baa.bio_analysis_attribute_id AND baal.ancestor_search_keyword_id=sk.search_keyword_id
						   AND baa.bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND sk.data_category='DISEASE'">
				<field name="DISEASE" column="search_keyword_id" />
            </entity>

            <entity name="siggene" query="SELECT distinct search_keyword_id SEARCH_KEYWORD_ID
                           FROM heat_map_results
                           WHERE bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND significant=1">
				<field name="SIGGENE" column="search_keyword_id" />
            </entity>

            <entity name="anysignificantgenes" query="SELECT case count(distinct search_keyword_id) when 0 then 0 else 1 end ANY_SIGNIFICANT_GENES
                           FROM heat_map_results
                           WHERE bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'
						   AND significant=1">
				<field name="ANY_SIGNIFICANT_GENES" column="any_significant_genes" />
            </entity>

            <entity name="allgene" query="SELECT distinct search_keyword_id SEARCH_KEYWORD_ID
                           FROM heat_map_results
                           WHERE bio_assay_analysis_id='${analysis.bio_assay_analysis_id}'">
				<field name="ALLGENE" column="search_keyword_id" />
            </entity>
	</entity>
    </document>
</dataConfig>



