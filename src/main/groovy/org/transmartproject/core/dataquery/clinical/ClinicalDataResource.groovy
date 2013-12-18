package org.transmartproject.core.dataquery.clinical

import org.transmartproject.core.dataquery.TabularResult
import org.transmartproject.core.doc.Experimental
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.querytool.QueryResult

@Experimental
public interface ClinicalDataResource {

    @Experimental
    TabularResult<ClinicalVariableColumn, PatientRow> retrieveData(QueryResult patientSet,
                                                                   List<ClinicalVariable> variables)

    @Experimental
    ClinicalVariable createClinicalVariable(Map<String, Object> parameters,
                                            String type) throws InvalidArgumentsException

}
