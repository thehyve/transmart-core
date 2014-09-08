package org.transmartproject.batch.clinical

import org.springframework.batch.item.ExecutionContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.transmartproject.batch.model.ColumnMapping
import org.transmartproject.batch.model.WordMapping

@Scope('job')
@Component
class ClinicalJobContextImpl implements ClinicalJobContext {

    @Value('#{jobParameters}')
    Map jobParameters

    //@Value('#{jobExecutionContext}') //cannot inject this as value, as we want the real context, not the immutable Map
    ExecutionContext jobExecutionContext

    List<ColumnMapping> columnMappings
    List<WordMapping> wordMappings
    //Map<File,List<String>> dataFileColumnsMap
    //Map<String,List<Mapping>> wordMappingsPerFile = [:]

}
