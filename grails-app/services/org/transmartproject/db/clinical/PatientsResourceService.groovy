package org.transmartproject.db.clinical

import org.transmartproject.core.dataquery.Patient
import org.transmartproject.core.dataquery.clinical.PatientsResource
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.querytool.Item
import org.transmartproject.core.querytool.Panel
import org.transmartproject.core.querytool.QueryDefinition
import org.transmartproject.db.i2b2data.PatientDimension

class PatientsResourceService implements PatientsResource {

    def patientSetQueryBuilderService

    def sessionFactory

    @Override
    Patient getPatientById(Long id) throws NoSuchResourceException {
        PatientDimension.get(id) ?:
                { throw new NoSuchResourceException("No patient with number $id") }()
    }

    @Override
    List<Patient> getPatients(OntologyTerm term) {

        def definition = new QueryDefinition([
                new Panel(
                        invert: false,
                        items:  [
                                new Item(conceptKey: term.key)
                        ]
                )
        ])

        def patientsSql = patientSetQueryBuilderService.buildPatientIdListQuery(definition)
        def patientsQuery = sessionFactory.currentSession.createSQLQuery patientsSql
        def patientIdList = patientsQuery.list()

        //this is a hack so integration tests work. for some reason the h2 schema doesn't have the right column type
        if (patientIdList.size() > 0 && patientIdList[0].getClass() != Long) {
            patientIdList = patientIdList.collect( {it as Long} )
        }
        PatientDimension.findAllByIdInList(patientIdList)
    }

}
