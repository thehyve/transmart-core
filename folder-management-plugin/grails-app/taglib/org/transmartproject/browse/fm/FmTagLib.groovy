package org.transmartproject.browse.fm

import annotation.AmTagDisplayValue
import annotation.AmTagItem
import annotation.MetaDataService
import grails.artefact.TagLibrary
import grails.gsp.TagLib
import org.transmart.biomart.ConceptCode

@TagLib
class FmTagLib implements TagLibrary {

    static namespace = 'fm'
    static returnObjectForTags = [
            'tagValues', 'tagValue', 'codes', 'viewValues',
            'checkSubjectLevelData'
    ]

    MetaDataService metaDataService
    def ontologyService
    FmFolderService fmFolderService

    Closure<List<AmTagDisplayValue>> tagValues = { attrs ->
        def folder = attrs.folder as FmFolder
        def amTagItem = attrs.amTagItem as AmTagItem
        metaDataService.getTagValues(folder, amTagItem)
    }

    Closure<AmTagDisplayValue> tagValue = { attrs ->
        def folder = attrs.folder as FmFolder
        def amTagItem = attrs.amTagItem as AmTagItem
        metaDataService.getTagValue(folder, amTagItem)
    }

    Closure<List<ConceptCode>> codes = { attrs ->
        def amTagItem = attrs.amTagItem as AmTagItem
        metaDataService.getCodes(amTagItem)
    }

    Closure viewValues = { attrs ->
        def fieldValue = attrs.fieldValue as String
        metaDataService.getViewValues(fieldValue)
    }

    Closure<Boolean> checkSubjectLevelData = { attrs ->
        def folder = attrs.folder as FmFolder
        ontologyService.checkSubjectLevelData(fmFolderService.getAssociatedAccession(folder))
    }

}
