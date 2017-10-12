package annotation

import grails.artefact.TagLibrary
import grails.gsp.TagLib
import org.transmart.biomart.ConceptCode
import org.transmartproject.browse.fm.FmFolder

@TagLib
class MetadataTagLib implements TagLibrary {

    static namespace = 'metadata'
    static returnObjectForTags = ['tagValues', 'tagValue', 'codes', 'viewValues']

    MetaDataService metaDataService

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

}
