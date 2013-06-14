package org.transmartproject.db.dataquery

import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.StatelessSession
import org.hibernate.impl.AbstractSessionImpl
import org.transmartproject.core.dataquery.DataQueryResource
import org.transmartproject.core.dataquery.acgh.RegionResult
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.constraints.ACGHRegionQuery

import static org.hibernate.ScrollMode.FORWARD_ONLY

class DataQueryResourceService implements DataQueryResource {

    def sessionFactory

    @Override
    RegionResult runACGHRegionQuery(ACGHRegionQuery spec, session) {
        if (!(session == null || session instanceof Session || session
                instanceof StatelessSession)) {
            throw new IllegalArgumentException('Expected session to be null ' +
                    'or an instance of type org.hibernate.Session or ' +
                    'org.hibernate.StatelessSession')
        }

        validateQuery(spec)

        session = session ?: sessionFactory.currentSession
        List<Assay> assays = getAssaysForACGHRegionQuery(spec, session)
        if (log.isDebugEnabled()) {
            log.debug("Found ${assays.size()} assays: " +
                    assays.collect {
                        "{id: $it.id, subjectId: $it.subjectId}"
                    }.join(", "))
        }

        getRegionResultForAssays(assays, session)
    }

    protected List<Assay> getAssaysForACGHRegionQuery(ACGHRegionQuery spec, AbstractSessionImpl session) {
        def whereClauses, params

        /* first obtain list of assays */
        whereClauses = []
        params = [:]
        populateWhereClauses(spec, whereClauses, params)

        def assayHQL = 'from DeSubjectSampleMapping assay\n'
        assayHQL <<= 'where ' + whereClauses.join("\nand ") + "\n"
        assayHQL <<= 'order by assay\n'

        createQuery(session, assayHQL, params).list()
    }

    protected RegionResult getRegionResultForAssays(final List<Assay> assays, AbstractSessionImpl session) {
        /* Then obtain the meat.
         *
         * Doing 'select acgh from ... inner join fetch acgh.region' should
         * be enough (see HHH-3528) so that acgh.region is loaded by the
         * query and such data is used when retrieving DeSubjectAcghData
         * .region. Unfortunately, I could not get it to work on Stateless
         * sessions, even though the generated SQL query does retrieve the
         * region data. I get a "proxies cannot be fetched by a stateless
         * session" (when using stateless sessions) when grails tries to
         * unwrap the hibernate proxy after trying to retrieve .region.
         * Therefore, RegionResultImpl does not use the .region property and
         * instead uses the region object returned alongside the
         * DeSubjectAcghData as result rows.
         *
         * By the way, it could be more efficient (some testing would be
         * necessary) to retrieve only the region id here and issue another
         * query before just to get the regions. This would minimize the
         * amount of data that the postgres server has to send us.
         */
        def mainHQL = '''
            select acgh, acgh.region
            from DeSubjectAcghData as acgh
            inner join acgh.assay assay
            where assay in (:assays)
            order by acgh.region.id, assay'''

        def mainQuery = createQuery(session, mainHQL, ['assays': assays]).scroll(FORWARD_ONLY)

        new RegionResultImpl(assays, mainQuery)
    }

    private void validateQuery(ACGHRegionQuery q) {
        def checkEmptiness = { it, name ->
            if (!it) {
                throw new IllegalArgumentException("$name not specified/empty")
            }
        }
        checkEmptiness(q.common, "query.common")
        checkEmptiness(q.common.studies, "query.common.studies")
        checkEmptiness(q.common.patientQueryResult, "query.common.patientQueryResult")
    }

    private Query createQuery(session, hql, params) {
        log.debug("Creating HQL query: $hql")
        log.debug("Parameters to bind: $params")

        Query assayQuery = session.createQuery(hql.toString())

        assayQuery.readOnly = true
        assayQuery.cacheable = false

        params.each { key, val ->
            if (val instanceof Collection) {
                assayQuery.setParameterList(key, val)
            } else {
                assayQuery.setParameter(key, val)
            }
        }

        assayQuery
    }

    private void populateWhereClauses(ACGHRegionQuery q,
                                      List whereClauses,
                                      Map params) {

        /* assay in the desired studies */
        whereClauses << 'assay.trialName in (:studies)'
        params['studies'] = q.common.studies

        /* assays correspond to patients in the result set */
        /* do not use assay.patient in ( select pset.patient ... ); hibernate
         * will unnecessarily do a join with patient_dimension */
        whereClauses <<
                '''assay.patient.id in (
                    select pset.patient.id
                    from QtPatientSetCollection pset
                    where pset.resultInstance = :queryResult
                )'''
        params['queryResult'] = q.common.patientQueryResult

        /* We treat empty lists the same as nulls here!
         * This means specifying an empty list for platforms will not return an
         * empty result set -- there simply won't be any filtering by platform
         * Idem for the rest
         */
        if (q.common.platforms) {
            whereClauses << 'assay.platform in (:platforms)'
            params['platforms'] = q.common.platforms
        }

        if (q.common.sampleCodes) {
            whereClauses << 'assay.sampleTypeCd in (:sampleCodes)'
            params['sampleCodes'] = q.common.sampleCodes
        }
        if (q.common.tissueCodes) {
            whereClauses << 'assay.tissueTypeCd in (:tissueCodes)'
            params['tissueCodes'] = q.common.tissueCodes
        }
        if (q.common.timepointCodes) {
            whereClauses << 'assay.timepointCd in (:timepointCodes)'
            params['timepointCodes'] = q.common.timepointCodes
        }
    }
}


