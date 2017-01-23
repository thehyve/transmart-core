package com.recomdata.transmart.rmodules

import grails.converters.JSON
import org.transmartproject.core.dataquery.assay.Assay
import org.transmartproject.core.dataquery.assay.SampleType
import org.transmartproject.core.dataquery.assay.Timepoint
import org.transmartproject.core.dataquery.assay.TissueType
import org.transmartproject.core.dataquery.highdim.HighDimensionDataTypeResource
import org.transmartproject.core.dataquery.highdim.HighDimensionResource
import org.transmartproject.core.dataquery.highdim.Platform
import org.transmartproject.core.dataquery.highdim.assayconstraints.AssayConstraint

class HighDimensionController {

    /* singleton so our cache is of any use */
    static scope = 'singleton'

    HighDimensionResource highDimensionResourceService

    @Lazy volatile JSON dataTypesMap = {
        def output
        output = highDimensionResourceService.knownTypes.collectEntries {
            def subResource =
                    highDimensionResourceService.getSubResourceForType(it)
            [
                    it,
                    [
                            assayConstraint: subResource.supportedAssayConstraints,
                            dataConstraints: subResource.supportedDataConstraints,
                            projections:     subResource.supportedProjections,
                    ]
            ]
        }
        output as JSON
    }()

    def knownDataTypes() {
        render dataTypesMap
    }

    def nodeDetails() {
        def constraints = []

        constraints << highDimensionResourceService.createAssayConstraint(
                AssayConstraint.DISJUNCTION_CONSTRAINT,
                subconstraints: [
                        (AssayConstraint.ONTOLOGY_TERM_CONSTRAINT):
                                (params.conceptKeys instanceof String[] ?
                                        params.conceptKeys :
                                        [params.conceptKeys]).collect {[concept_key: it]} ])

        def assayMultiMap = highDimensionResourceService.
                getSubResourcesAssayMultiMap(constraints)

        def result = assayMultiMap.collectEntries { HighDimensionDataTypeResource dataTypeResource,
                                                    Collection<Assay> assays ->
            def details = [
                    platforms:   new HashSet<Platform>(),
                    trialNames:  new HashSet<String>(),
                    timepoints:  new HashSet<Timepoint>(),
                    tissueTypes: new HashSet<TissueType>(),
                    sampleTypes: new HashSet<SampleType>(),
            ]

            [
                    dataTypeResource.dataTypeName,
                    assays.inject(details, { accum, Assay assay ->
                        accum.platforms   << platformToMap(assay.platform)
                        accum.trialNames  << assay.trialName
                        accum.timepoints  << assay.timepoint
                        accum.tissueTypes << assay.tissueType
                        accum.sampleTypes << assay.sampleType
                        accum
                    })
            ]
        }

        render result as JSON
    }

    private platformToMap(Platform p) {
        Platform.metaClass.properties.
                collect { it.name }.
                minus(['class', 'template']).
                collectEntries {
                    [  it, p."$it" ]
                }
    }

}
