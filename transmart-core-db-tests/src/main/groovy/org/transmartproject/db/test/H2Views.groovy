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

import groovy.util.logging.Slf4j
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SessionImplementor
import org.springframework.beans.factory.annotation.Autowired

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

/**
 * This class is for integration test purposes, but has to be here due to
 * technical limitations. In particular, when resources.groovy is run, neither
 * the script's classloader nor the context classloader know anything about
 * the test classpath.
 */
@Slf4j
class H2Views {

    @Autowired
    SessionFactory sessionFactory

    @PostConstruct
    protected void init() {
        Session session = sessionFactory.openSession()

        try {
            if (session instanceof SessionImplementor && !isH2(session)) {
                log.warn 'Skip test resources creation as database is not H2.'
                return
            }
            log.info 'Executing H2Views init actions'

            createSearchBioMkrCorrelView(session)
            createSearchAuthUserSecAccessV(session)
            createBioMarkerCorrelMv(session)
            createSubPathwayCorrelView(session)
            createSuperPathwayCorrelView(session)
            createModifierDimensionView(session)
            createDeVariantSummaryDetailGene(session)
        } finally {
            session.flush()
            session.close()
        }
    }


    @PreDestroy
    protected void destroy() {
        Session session = sessionFactory.openSession()

        try {
            if (session instanceof SessionImplementor && !isH2(session)) {
                log.warn 'Skip test resources cleanup as the database is not H2.'
                return
            }

            log.info 'Cleaning resources created in H2Views'

            dropViewIfExists(session, 'SEARCHAPP', 'SEARCH_BIO_MKR_CORREL_VIEW')
            dropViewIfExists(session, 'SEARCHAPP', 'SEARCH_AUTH_USER_SEC_ACCESS_V')
            dropViewIfExists(session, 'BIOMART', 'BIO_MARKER_CORREL_MV')
            dropViewIfExists(session, 'BIOMART', 'BIO_METAB_SUBPATHWAY_VIEW')
            dropViewIfExists(session, 'BIOMART', 'BIO_METAB_SUPERPATHWAY_VIEW')
            dropViewIfExists(session, 'I2B2DEMODATA', 'MODIFIER_DIMENSION_VIEW')
            dropViewIfExists(session, 'DEAPP', 'DE_VARIANT_SUMMARY_DETAIL_GENE')

        } finally {
            session.flush()
            session.close()
        }
    }

    protected static createBioMarkerCorrelMv(Session session) {
        dropTableIfExist(session, 'BIOMART', 'BIO_MARKER_CORREL_MV')

        session.createSQLQuery('''
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
               b.bio_marker_type = 'METABOLITE'
            UNION
            SELECT DISTINCT
                b.bio_marker_id,
                b.bio_marker_id AS asso_bio_marker_id,
                'TRANSCRIPT' AS correl_type,
                10 AS mv_id
            FROM
                biomart.bio_marker b
            WHERE
               b.bio_marker_type = 'TRANSCRIPT'
            UNION
            SELECT DISTINCT
                c.bio_data_id AS bio_marker_id,
                c.asso_bio_data_id AS asso_bio_marker_id,
                'GENE TO TRANSCRIPT' AS correl_type,
                11 AS mv_id
            FROM
                biomart.bio_marker b,
                biomart.bio_data_correlation c,
                biomart.bio_data_correl_descr d
            WHERE
                b.bio_marker_id = c.bio_data_id
                AND c.bio_data_correl_descr_id = d.bio_data_correl_descr_id
                AND d.correlation = 'GENE TO TRANSCRIPT';''').executeUpdate()

    }

    protected static createSearchAuthUserSecAccessV(Session session) {
        dropTableIfExist(session, 'SEARCHAPP', 'SEARCH_AUTH_USER_SEC_ACCESS_V')

        log.info 'Creating SEARCHAPP.SEARCH_AUTH_USER_SEC_ACCESS_V'

        session.createSQLQuery('''
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
                sag.group_category = 'EVERYONE_GROUP';''').executeUpdate()
    }

    protected static createSearchBioMkrCorrelView(Session session) {
        dropTableIfExist(session, 'SEARCHAPP', 'SEARCH_BIO_MKR_CORREL_VIEW')

        log.info 'Creating SEARCHAPP.SEARCH_BIO_MKR_CORREL_VIEW'

        session.createSQLQuery('''
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
                        AND i.bio_assay_feature_group_id IS NOT NULL ) A; ''').executeUpdate()
    }

    protected static createSubPathwayCorrelView(Session session) {
        dropTableIfExist(session, 'BIOMART', 'BIO_METAB_SUBPATHWAY_VIEW')

        log.info 'Creating BIOMART.BIO_METAB_SUBPATHWAY_VIEW'

        session.createSQLQuery('''
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
                    B.primary_external_id = M.hmdb_id);''').executeUpdate()
    }

    protected static createSuperPathwayCorrelView(Session session) {
        dropTableIfExist(session, 'BIOMART', 'BIO_METAB_SUPERPATHWAY_VIEW')

        log.info 'Creating BIOMART.BIO_METAB_SUPERPATHWAY_VIEW'

        session.createSQLQuery('''
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
                    B.primary_external_id = M.hmdb_id);''').executeUpdate()
    }

    protected static createModifierDimensionView(Session session) {
        dropTableIfExist(session, 'I2B2DEMODATA', 'MODIFIER_DIMENSION_VIEW')

        log.info 'Creating I2B2DEMODATA.MODIFIER_DIMENSION_VIEW'

        session.createSQLQuery('''
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
                    MD.modifier_cd = MM.modifier_cd)''').executeUpdate()
    }

    protected static createDeVariantSummaryDetailGene(Session session) {
        dropTableIfExist(session, 'DEAPP', 'DE_VARIANT_SUMMARY_DETAIL_GENE')

        log.info 'Creating DEAPP.DE_VARIANT_SUMMARY_DETAIL_GENE'

        session.createSQLQuery('''
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
                geneid.info_name = 'GID' ''').executeUpdate()
    }

    protected static boolean dropTableIfExist(Session session, String schema, String tableName) {
        session.createSQLQuery("DROP TABLE IF EXISTS $schema.$tableName" as String).executeUpdate()
    }

    protected static boolean dropViewIfExists(Session session, String schema, String viewName) {
        session.createSQLQuery("DROP VIEW IF EXISTS $schema.$viewName" as String).executeUpdate()
    }

    protected static boolean isH2(SessionImplementor session) {
        session.connection().metaData.databaseProductName.equalsIgnoreCase('h2')
    }
}