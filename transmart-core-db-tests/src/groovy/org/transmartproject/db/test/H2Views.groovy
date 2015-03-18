/*
 * Copyright © 2013-2014 The Hyve B.V.
 *
 * This file is part of transmart-core-db.
 *
 * Transmart-core-db is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * transmart-core-db.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.transmartproject.db.test

import groovy.sql.Sql
import groovy.util.logging.Log4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

import javax.annotation.PostConstruct
import javax.sql.DataSource

import static org.transmartproject.db.test.H2Views.ObjectStatus.*

/**
 * This class is for integration test purposes, but has to be here due to
 * technical limitations. In particular, when resources.groovy is run, neither
 * the script's classloader nor the context classloader know anything about
 * the test classpath.
 */
@Log4j
class H2Views {

    @Autowired
    @Qualifier('dataSource')
    DataSource dataSource

    private Sql sql

    @PostConstruct
    void init() {
        this.sql = new Sql(dataSource.connection)

        try {
            if (!isH2()) {
                return
            }

            log.info 'Executing H2Views init actions'

            createSearchBioMkrCorrelView()
            createSearchAuthUserSecAccessV()
            createBioMarkerCorrelMv()
            createSubPathwayCorrelView()
            createSuperPathwayCorrelView()
            createI2b2TrialNodes()
            createModifierDimensionView()
            createDeVariantSummaryDetailGene()
        } finally {
            this.sql.close()
        }
    }

    void createBioMarkerCorrelMv() {
        if (handleCurrentState('BIOMART', 'BIO_MARKER_CORREL_MV')) {
            return
        }

        sql.execute '''
            CREATE VIEW BIOMART.BIO_MARKER_CORREL_MV (
                BIO_MARKER_ID,
                ASSO_BIO_MARKER_ID,
                CORREL_TYPE,
                MV_ID ) AS
            SELECT DISTINCT
                b.bio_marker_id,
                b.bio_marker_id AS asso_bio_marker_id,
                'GENE' AS correl_type,
                1 AS mv_id
            FROM
                    biomart.bio_marker b
            WHERE
                    b.bio_marker_type = 'GENE'
            UNION
            SELECT DISTINCT
                b.bio_marker_id,
                b.bio_marker_id AS asso_bio_marker_id,
                'PROTEIN' AS correl_type,
                4 AS mv_id
            FROM
                biomart.bio_marker b
            WHERE
               b.bio_marker_type = 'PROTEIN'
            UNION
            SELECT DISTINCT
                b.bio_marker_id,
                b.bio_marker_id AS asso_bio_marker_id,
                'MIRNA' AS correl_type,
                7 AS mv_id
            FROM
                biomart.bio_marker b
            WHERE
               b.bio_marker_type = 'MIRNA'
            UNION
            SELECT DISTINCT
                c.bio_data_id AS bio_marker_id,
                c.asso_bio_data_id AS asso_bio_marker_id,
                'PATHWAY GENE' AS correl_type,
                2 AS mv_id
            FROM
                biomart.bio_marker b,
                biomart.bio_data_correlation c,
                biomart.bio_data_correl_descr d
            WHERE
                b.bio_marker_id = c.bio_data_id
                AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
                AND b.primary_source_code <> 'ARIADNE'
                AND d.correlation = 'PATHWAY GENE'
            UNION
            SELECT DISTINCT
                c.bio_data_id AS bio_marker_id,
                c.asso_bio_data_id AS asso_bio_marker_id,
                'HOMOLOGENE_GENE' AS correl_type,
                3 AS mv_id
            FROM
                biomart.bio_marker b,
                biomart.bio_data_correlation c,
                biomart.bio_data_correl_descr d
            WHERE
                b.bio_marker_id = c.bio_data_id
                AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
                AND d.correlation = 'HOMOLOGENE GENE'
            UNION
            SELECT DISTINCT
                c.bio_data_id AS bio_marker_id,
                c.asso_bio_data_id AS asso_bio_marker_id,
                'PROTEIN TO GENE' AS correl_type,
                5 AS mv_id
            FROM
                biomart.bio_marker b,
                biomart.bio_data_correlation c,
                biomart.bio_data_correl_descr d
            WHERE
                b.bio_marker_id = c.bio_data_id
                AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
                AND d.correlation = 'PROTEIN TO GENE'
            UNION
            SELECT DISTINCT
                c.bio_data_id AS bio_marker_id,
                c.asso_bio_data_id AS asso_bio_marker_id,
                'GENE TO PROTEIN' AS correl_type,
                6 AS mv_id
            FROM
                biomart.bio_marker b,
                biomart.bio_data_correlation c,
                biomart.bio_data_correl_descr d
            WHERE
                b.bio_marker_id = c.bio_data_id
                AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
                AND d.correlation = 'GENE TO PROTEIN'
            UNION
            SELECT
                c1.bio_data_id,
                c2.asso_bio_data_id,
                'PATHWAY TO PROTEIN' as correl_type,
                8 AS mv_id
            FROM
                bio_data_correlation c1
                INNER JOIN bio_data_correlation c2 ON c1.asso_bio_data_id = c2.bio_data_id
                INNER JOIN bio_data_correl_descr d1 ON c1.bio_data_correl_descr_id = d1.bio_data_correl_descr_id
                INNER JOIN bio_data_correl_descr d2 ON c2.bio_data_correl_descr_id = d2.bio_data_correl_descr_id
                WHERE d1.correlation = 'PATHWAY GENE'
                AND d2.correlation = 'GENE TO PROTEIN'
            UNION
            SELECT DISTINCT
                b.bio_marker_id,
                b.bio_marker_id AS asso_bio_marker_id,
                'METABOLITE' AS correl_type,
                9 AS mv_id
            FROM
                biomart.bio_marker b
            WHERE
               b.bio_marker_type = 'METABOLITE';'''
    }

    void createSearchAuthUserSecAccessV() {
        if (handleCurrentState('SEARCHAPP', 'SEARCH_AUTH_USER_SEC_ACCESS_V')) {
            return
        }

        log.info 'Creating SEARCHAPP.SEARCH_AUTH_USER_SEC_ACCESS_V'

        sql.execute '''
            CREATE VIEW SEARCHAPP.SEARCH_AUTH_USER_SEC_ACCESS_V(
                search_auth_user_sec_access_id,
                search_auth_user_id,
                search_secure_object_id,
                search_sec_access_level_id
            ) AS
            SELECT
                sasoa.auth_sec_obj_access_id,
                sasoa.auth_principal_id,
                sasoa.secure_object_id,
                sasoa.secure_access_level_id
            FROM
                searchapp.search_auth_user sau
                INNER JOIN searchapp.search_auth_sec_object_access sasoa ON
                    sau.id = sasoa.auth_principal_id
            UNION
            SELECT
                sasoa.auth_sec_obj_access_id,
                sagm.auth_user_id,
                sasoa.secure_object_id,
                sasoa.secure_access_level_id
            FROM
                searchapp.search_auth_sec_object_access sasoa
                INNER JOIN searchapp.search_auth_group_member sagm
                    ON sagm.auth_group_id = sasoa.auth_principal_id
                INNER JOIN searchapp.search_auth_group sag
                    ON sag.id = sagm.auth_group_id
            UNION
            SELECT
                sasoa.auth_sec_obj_access_id,
                CAST(NULL AS BIGINT),
                sasoa.secure_object_id,
                sasoa.secure_access_level_id
            FROM
                searchapp.search_auth_group sag
                INNER JOIN searchapp.search_auth_sec_object_access sasoa
                    ON sag.id = sasoa.auth_principal_id
            WHERE
                sag.group_category = 'EVERYONE_GROUP';'''
    }

    void createSearchBioMkrCorrelView() {
        if (handleCurrentState('SEARCHAPP', 'SEARCH_BIO_MKR_CORREL_VIEW')) {
            return
        }

        log.info 'Creating SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW'

        sql.execute '''
            CREATE VIEW SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW (
                DOMAIN_OBJECT_ID,
                ASSO_BIO_MARKER_ID,
                CORREL_TYPE,
                VALUE_METRIC,
                MV_ID ) AS
            SELECT
                domain_object_id,
                asso_bio_marker_id,
                correl_type,
                value_metric,
                mv_id
            FROM (
                    SELECT
                        i.SEARCH_GENE_SIGNATURE_ID AS domain_object_id,
                        i.BIO_MARKER_ID AS asso_bio_marker_id,
                        'GENE_SIGNATURE_ITEM' AS correl_type,
                        CASE
                            WHEN i.FOLD_CHG_METRIC IS NULL THEN 1
                            ELSE i.FOLD_CHG_METRIC
                        END AS value_metric,
                        1 AS mv_id
                    FROM
                        SEARCHAPP.SEARCH_GENE_SIGNATURE_ITEM i,
                        SEARCHAPP.SEARCH_GENE_SIGNATURE gs
                    WHERE
                        i.SEARCH_GENE_SIGNATURE_ID = gs.SEARCH_GENE_SIGNATURE_ID
                        AND gs.DELETED_FLAG IS FALSE
                        AND i.bio_marker_id IS NOT NULL
                    UNION
                        ALL
                    SELECT
                        i.SEARCH_GENE_SIGNATURE_ID AS domain_object_id,
                        bada.BIO_MARKER_ID AS asso_bio_marker_id,
                        'GENE_SIGNATURE_ITEM' AS correl_type,
                        CASE
                            WHEN i.FOLD_CHG_METRIC IS NULL THEN 1
                            ELSE i.FOLD_CHG_METRIC
                        END AS value_metric,
                        2 AS mv_id
                    FROM
                        SEARCHAPP.SEARCH_GENE_SIGNATURE_ITEM i,
                        SEARCHAPP.SEARCH_GENE_SIGNATURE gs,
                        BIOMART.BIO_ASSAY_DATA_ANNOTATION bada
                    WHERE
                        i.SEARCH_GENE_SIGNATURE_ID = gs.SEARCH_GENE_SIGNATURE_ID
                        AND gs.DELETED_FLAG IS FALSE
                        AND bada.bio_assay_feature_group_id = i.bio_assay_feature_group_id
                        AND i.bio_assay_feature_group_id IS NOT NULL ) A; '''
    }

    void createSubPathwayCorrelView() {
        if (handleCurrentState('BIOMART', 'BIO_METAB_SUBPATHWAY_VIEW')) {
            return
        }

        log.info 'Creating BIOMART.BIO_METAB_SUBPATHWAY_VIEW'

        sql.execute '''
            CREATE VIEW BIOMART.BIO_METAB_SUBPATHWAY_VIEW(
                SUBPATHWAY_ID,
                ASSO_BIO_MARKER_ID,
                CORREL_TYPE) AS
            SELECT
                SP.id,
                B.bio_marker_id,
                'SUBPATHWAY TO METABOLITE'
            FROM
                deapp.de_metabolite_sub_pathways SP
                INNER JOIN deapp.de_metabolite_sub_pway_metab J ON (SP.id = J.sub_pathway_id)
                INNER JOIN deapp.de_metabolite_annotation M ON (M.id = J.metabolite_id)
                INNER JOIN biomart.bio_marker B ON (
                    B.bio_marker_type = 'METABOLITE' AND
                    B.primary_external_id = M.hmdb_id);'''
    }

    void createSuperPathwayCorrelView() {
        if (handleCurrentState('BIOMART', 'BIO_METAB_SUPERPATHWAY_VIEW')) {
            return
        }

        log.info 'Creating BIOMART.BIO_METAB_SUPERPATHWAY_VIEW'

        sql.execute '''
            CREATE VIEW BIOMART.BIO_METAB_SUPERPATHWAY_VIEW(
                SUPERPATHWAY_ID,
                ASSO_BIO_MARKER_ID,
                CORREL_TYPE) AS
            SELECT
                SUPP.id,
                B.bio_marker_id,
                'SUPERPATHWAY TO METABOLITE\'
            FROM
                deapp.de_metabolite_super_pathways SUPP
                INNER JOIN deapp.de_metabolite_sub_pathways SUBP ON (SUPP.id = SUBP.super_pathway_id)
                INNER JOIN deapp.de_metabolite_sub_pway_metab J ON (SUBP.id = J.sub_pathway_id)
                INNER JOIN deapp.de_metabolite_annotation M ON (M.id = J.metabolite_id)
                INNER JOIN biomart.bio_marker B ON (
                    B.bio_marker_type = 'METABOLITE' AND
                    B.primary_external_id = M.hmdb_id);'''
    }

    void createI2b2TrialNodes() {
        if (handleCurrentState('I2B2METADATA', 'I2B2_TRIAL_NODES')) {
            return
        }

        log.info 'Creating I2B2METADATA.I2B2_TRIAL_NODES'

        sql.execute '''
            CREATE VIEW I2B2METADATA.I2B2_TRIAL_NODES(C_FULLNAME, TRIAL) AS
            SELECT
                A.c_fullname,
                substr (A.c_comment, 7) -- remove 'trial:' prefix
            FROM
                i2b2metadata.i2b2 AS A
                JOIN (
                    SELECT
                        c_comment,
                        MIN(length(c_fullname)) AS min_length
                    FROM
                        i2b2metadata.i2b2
                    GROUP BY c_comment) B ON (
                        A.c_comment = B.c_comment
                        AND length (A.c_fullname) = B.min_length);'''
    }

    void createModifierDimensionView() {
        if (handleCurrentState('I2B2DEMODATA', 'MODIFIER_DIMENSION_VIEW')) {
            return
        }

        log.info 'Creating I2B2DEMODATA.MODIFIER_DIMENSION_VIEW'

        sql.execute '''
            CREATE VIEW I2B2DEMODATA.MODIFIER_DIMENSION_VIEW AS
            SELECT
                MD.modifier_path,
                MD.modifier_cd,
                MD.name_char,
                MD.sourcesystem_cd,
                MD.modifier_level,
                MD.modifier_node_type,
                MM.valtype_cd,
                MM.std_units,
                MM.visit_ind
            FROM
                I2B2DEMODATA.MODIFIER_DIMENSION MD
                LEFT JOIN I2B2DEMODATA.MODIFIER_METADATA MM ON (
                    MD.modifier_cd = MM.modifier_cd)'''
    }

    void createDeVariantSummaryDetailGene() {
        if (handleCurrentState('DEAPP', 'DE_VARIANT_SUMMARY_DETAIL_GENE')) {
            return
        }

        log.info 'Creating DEAPP.DE_VARIANT_SUMMARY_DETAIL_GENE'

        sql.execute '''
            CREATE OR REPLACE VIEW DEAPP.DE_VARIANT_SUMMARY_DETAIL_GENE AS
            SELECT summary.variant_subject_summary_id,
                summary.chr,
                summary.pos,
                summary.dataset_id,
                summary.subject_id,
                summary.rs_id,
                summary.variant,
                summary.variant_format,
                summary.variant_type,
                summary.reference,
                summary.allele1,
                summary.allele2,
                summary.assay_id,
                detail.ref,
                detail.alt,
                detail.qual,
                detail.filter,
                detail.info,
                detail.format,
                detail.variant_value,
                genesymbol.text_value AS gene_name,
                geneid.text_value AS gene_id
            FROM deapp.de_variant_subject_summary summary
            JOIN deapp.de_variant_subject_detail detail ON
                detail.dataset_id = summary.dataset_id AND
                detail.chr = summary.chr AND
                detail.pos = summary.pos AND
                detail.rs_id = summary.rs_id
            LEFT JOIN deapp.de_variant_population_data genesymbol ON
                genesymbol.dataset_id = summary.dataset_id AND
                genesymbol.chr = summary.chr AND
                genesymbol.pos = summary.pos AND
                genesymbol.info_name = 'GS'
            LEFT JOIN deapp.de_variant_population_data geneid ON
                geneid.dataset_id = summary.dataset_id AND
                geneid.chr = summary.chr AND
                geneid.pos = summary.pos AND
                geneid.info_name = 'GID' '''
    }

    enum ObjectStatus {
        IS_VIEW,
        IS_TABLE,
        DOES_NOT_EXIST
    }

    ObjectStatus getCurrentStatus(String schema, String viewName) {
        def res

        res = sql.firstRow """
            SELECT EXISTS(
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.VIEWS
                WHERE TABLE_SCHEMA = $schema AND TABLE_NAME = $viewName)"""
        if (res[0]) {
            log.debug "Object $schema.$viewName is a already view"
            return IS_VIEW
        }

        res = sql.firstRow """
            SELECT EXISTS(
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = $schema AND TABLE_NAME = $viewName)"""
        if (res[0]) {
            log.debug "Object $schema.$viewName is a table"
            return IS_TABLE
        }

        log.debug "Object $schema.$viewName does not exist"
        DOES_NOT_EXIST
    }

    Boolean handleCurrentState(String schema, String viewName) {
        switch (getCurrentStatus(schema, viewName)) {
            case DOES_NOT_EXIST:
                return false
            case IS_TABLE:
                log.info "Dropping table $schema.$viewName because we are " +
                        "creating a view with that name"
                sql.execute("DROP TABLE $schema.$viewName" as String)
                return false
            case IS_VIEW:
                return true
        }
    }

    Boolean isH2() {
        sql.connection.metaData.databaseProductName.equalsIgnoreCase('h2')
    }
}
