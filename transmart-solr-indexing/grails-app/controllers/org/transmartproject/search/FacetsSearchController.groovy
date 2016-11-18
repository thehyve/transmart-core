package org.transmartproject.search

import grails.converters.JSON
import grails.validation.Validateable
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList
import org.apache.solr.common.SolrException
import org.grails.databinding.BindUsing
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.transmart.biomart.BioMarker
import org.transmartproject.core.concept.ConceptFullName
import org.transmartproject.core.exceptions.InvalidArgumentsException
import org.transmartproject.core.exceptions.InvalidRequestException
import org.transmartproject.core.exceptions.UnexpectedResultException
import org.transmartproject.core.ontology.ConceptsResource
import org.transmartproject.core.ontology.OntologyTerm
import org.transmartproject.core.ontology.Study
import org.transmartproject.search.indexing.FacetsIndexingService
import org.transmartproject.search.indexing.FacetsQueryingService
import org.transmartproject.search.indexing.TermCount

import java.util.regex.Matcher
import java.util.regex.Pattern

import static org.transmartproject.search.indexing.FacetsIndexingService.*

class FacetsSearchController {

    static scope = 'singleton'

    private static final Pattern KEY_CODE_PATTERN = Pattern.compile('(?<=\\A\\\\\\\\)[^\\\\]+')

    FacetsQueryingService facetsQueryingService

    @Autowired
    ConceptsResource conceptsResource

    private static final String PSEUDO_FIELD_GENE_LIST = '__gene_list'
    private static final String PSEUDO_FIELD_GENE_SIGNATURE = '__gene_signature'
    private static final String PSEUDO_FIELD_PATHWAY = '__pathway'
    private static final String PSEUDO_FIELD_ALL = '*'

    private static final PSEUDO_FIELDS = [PSEUDO_FIELD_GENE_LIST,
                                          PSEUDO_FIELD_GENE_SIGNATURE,
                                          PSEUDO_FIELD_PATHWAY,
                                          PSEUDO_FIELD_ALL,]

    private static final Pattern LUCENE_SPECIAL_CHARACTER = ~/[\Q+-&|!(){}[]^"~*?:\\E]/
    private static final int MAX_RESULTS = 100

    // for faceting
    def getFilterCategories() {
        render facetCountsToOutputMap(facetsQueryingService.getTopTerms(params.get('requiredField'))) as JSON
    }

    // Return search categories for the drop down
    def getSearchCategories() {
        render facetsQueryingService.allDisplaySettings.findAll { e ->
            !e.value.hideFromListings
        }.collectEntries { e ->
            [e.key, e.value.displayName]
        } as JSON
    }

    def autocomplete(AutoCompleteCommand autoCompleteCommand) {
        if (autoCompleteCommand.hasErrors()) {
            throw new InvalidRequestException("bad parameters: $autoCompleteCommand.errors")
        }

        def allFacetFields = facetsQueryingService.allFacetFields
        if (!(autoCompleteCommand.category in allFacetFields)
                && autoCompleteCommand.category != PSEUDO_FIELD_ALL) {
            render([] as JSON)
            return
        }

        def q = new SolrQuery()
        if (autoCompleteCommand.category != PSEUDO_FIELD_ALL) {
            q.addFacetField(autoCompleteCommand.category)
        } else {
            q.addFacetField(*allFacetFields)
        }
        q.rows = 0
        q.facetLimit = 10
        q.facetMinCount = 1
        // we have to filter the results ourselves, as facetPrefix is case-sensitive
        //q.facetPrefix = autoCompleteCommand.term
        def replaceWithLowercaseField = { it.replaceFirst(/_s\z/, '_l') }
        def facetsCommand = new GetFacetsCommand(
                requiredField: autoCompleteCommand.requiredField,
                operator: 'OR',
                fieldTerms: [
                        (replaceWithLowercaseField(autoCompleteCommand.category)): new FieldTerms(
                                operator: 'OR',
                                searchTerms: [
                                        new SearchTerm(luceneTerm: escapeSolrLiteral(autoCompleteCommand.term) + '*')]
                        )])
        q.query = commandToQueryString(facetsCommand,
                allFacetFields.collect { replaceWithLowercaseField it }, true)

        // execute query
        QueryResponse resp
        try {
            resp = facetsQueryingService.query(q)
        } catch (SolrException soe) {
            throw new UnexpectedResultException(soe);
        }

        def result = facetsQueryingService.parseFacetCounts(resp).collect { String field, SortedSet<TermCount> terms ->
            terms
                    .findAll {
                        it.term.toLowerCase(Locale.ENGLISH)
                                .startsWith(autoCompleteCommand.term.toLowerCase(Locale.ENGLISH))
                    }.collect {
                        [category: field,
                        value: it.term,
                        count: it.count]
                    }
        }.flatten()

        render result as JSON
    }

    def getFacetResults(GetFacetsCommand command) {
        if (command.hasErrors()) {
            throw new InvalidRequestException("bad parameters: $command.errors")
        }

        // expand __gene_list, __gene_signature and __pathway
        command.fieldTerms.findAll { e ->
            e.key == PSEUDO_FIELD_GENE_LIST || e.key == PSEUDO_FIELD_GENE_SIGNATURE
        }.each { e ->
            e.value.searchTerms = e.value.searchTerms.collect { SearchTerm term ->
                term.literalTerm.collect {
                    val -> expandGeneSignature(val)
                }
            }.flatten()
        }
        command.fieldTerms.findAll { e ->
            e.key == PSEUDO_FIELD_PATHWAY
        }.each { e ->
            e.value.searchTerms = e.value.searchTerms.collect { SearchTerm term ->
                term.literalTerm.collect {
                    val -> expandPathway(val)
                }
            }.flatten()
        }

        // build query
        def allFields = facetsQueryingService.allDisplaySettings.keySet()
        def q = new SolrQuery()
        q.addFacetField(*facetsQueryingService.allFacetFields)
        q.rows = MAX_RESULTS
        q.query = commandToQueryString command, allFields

        // execute query
        QueryResponse resp
        try {
            resp = facetsQueryingService.query(q)
        } catch (SolrException soe) {
            throw new UnexpectedResultException(soe)
        }

        // format output
        render([
                numFound   : resp.results.numFound,
                docs       : resp.results,
                conceptKeys: extractConceptKeys(resp.results),
                folderIds  : extractFolderIds(resp.results),
                facets     : facetCountsToOutputMap(facetsQueryingService.parseFacetCounts(resp))
        ] as JSON)
    }

    @Cacheable('misc_cache')
    protected Map<ConceptFullName, Study> getCategoriesFullNameMap() {
        conceptsResource.allCategories.collectEntries {
            [new ConceptFullName(it.fullName), it]
        }
    }

    private List extractFolderIds(SolrDocumentList documentList) {
        documentList.collect {
            it.getFieldValue(FIELD_NAME_FOLDER_ID)
        }.findAll().unique()
    }


    private List extractConceptKeys(SolrDocumentList documentList) {
        def catsFullNameMap = categoriesFullNameMap
        documentList
                .collect {
            it.getFieldValue(FacetsIndexingService.FIELD_NAME_CONCEPT_PATH)
        }
        .findAll()
                .unique()
                .collect {
            def conceptKey = guessConceptKey(it, catsFullNameMap)
            if (!conceptKey) {
                log.info("Could not determine concept key for $it")
            }
            conceptKey
        }.findAll()
    }

    private String guessConceptKey(String conceptPath, Map<ConceptFullName, Study> catsFullNameMap) {
        // take the study with whose concept full name the the overlap is larger
        def conceptFullName = new ConceptFullName(conceptPath)
        def r = catsFullNameMap.collectEntries { ConceptFullName categoryFullName, OntologyTerm t ->
            int i = 0
            for (; i < Math.min(conceptFullName.length, categoryFullName.length); i++) {
                if (conceptFullName[i] != categoryFullName[i]) {
                    break
                }
            }
            [t, i]
        }
        OntologyTerm candidateCategory = r.max { it.value }?.key
        if (candidateCategory == null) {
            return null
        }

        Matcher m = KEY_CODE_PATTERN.matcher(candidateCategory.key)
        if (m.find()) {
            "\\\\${m.group(0)}$conceptPath"
        }
    }

    private static escapeSolrLiteral(String s) {
        s.replaceAll LUCENE_SPECIAL_CHARACTER, { "\\$it" }
    }

    private List<String> expandGeneSignature(String signatureUniqueId) {
        BioMarker.executeQuery('''
            select items.bioMarker.name from GeneSignature s inner join s.geneSigItems items
            where s.uniqueId = :signatureUniqueId
        ''', [signatureUniqueId: signatureUniqueId])
    }

    private List<String> expandPathway(String pathway) {
        BioMarker.executeQuery('''
            select bio.bio_marker_name
            from org.transmart.searchapp.SearchKeyword sk,
            org.transmart.biomart.BioMarkerCorrelationMV mv,
            org.transmart.biomart.BioMarker bio
            where sk.dataCategory = 'PATHWAY' and sk.bioDataId = mv.bioMarkerId
            and bio.bioMarkerType = 'GENE' and mv.assoBioMarkerId = bio.id
            and mv.correlType = 'PATHWAY GENE'
            and sk.uniqueId = :uniqueId
        ''', [uniqueId: pathway])
    }

    /*
     * The alternativeAllMode (used for autocomplete assumes all queries are string queries)
     * and expands * exactly to allFields, without any extra logic
     */
    private String commandToQueryString(GetFacetsCommand command,
                                        Collection<String> allFields,
                                        alternativeAllMode = false) {
        def userString = command.fieldTerms.collect { String fieldName, FieldTerms fieldTerms ->
            if (!(fieldName in allFields) && fieldName != PSEUDO_FIELD_ALL && fieldName != FIELD_NAME_ID) {
                throw new InvalidArgumentsException("No such field: $fieldName")
            }

            if (fieldName != PSEUDO_FIELD_ALL) {
                def s = fieldTerms.searchTerms.collect { SearchTerm searchTerm ->
                    searchTerm.luceneTerm ?: "\"${escapeSolrLiteral(searchTerm.literalTerm)}\""
                }.join(" ${fieldTerms.operator} ")
                "$fieldName:(" + s + ')'
            } else if (alternativeAllMode) {
                def s = fieldTerms.searchTerms.collect { SearchTerm searchTerm ->
                    def right = searchTerm.luceneTerm ?: "\"${escapeSolrLiteral(searchTerm.literalTerm)}\""

                    allFields
                            .collect { "$it:(" + right + ')' }.join(' OR ')
                }.join(" ${fieldTerms.operator} ")

                "($s)"
            } else { /* field ALL, not alternative all mode */
                def numberFields = allFields.findAll { it =~ /_[if]\z/ } + FIELD_NAME_FOLDER_ID
                def textFields = allFields.findAll({ it =~ /_[st]\z/ }) +
                        [FIELD_NAME_ID, FIELD_NAME_CONCEPT_PATH, FIELD_NAME_TEXT]

                def s = fieldTerms.searchTerms.collect { SearchTerm searchTerm ->
                    def right = searchTerm.luceneTerm ?: "\"${escapeSolrLiteral(searchTerm.literalTerm)}\""

                    def isSearchForNumber = (searchTerm.luceneTerm ?: searchTerm.literalTerm)?.isDouble() ||
                            right.find() /* 1st char */ == '['

                    (isSearchForNumber ? numberFields : textFields)
                            .collect { "$it:(" + right + ')' }.join(' OR ')
                }.join(" ${fieldTerms.operator} ")

                "($s)"
            }
        }.join(" ${command.operator} ")

        if (command.requiredField) {
            "${command.requiredField}:* AND ($userString)"
        } else {
            userString
        }
    }

    private List facetCountsToOutputMap(LinkedHashMap<String, SortedSet<TermCount>> originalMap) {
        def displaySettings = facetsQueryingService.allDisplaySettings
        originalMap.collect { e ->
            [
                    category: [
                            field      : e.key,
                            displayName: displaySettings[e.key].displayName,
                    ],
                    choices : e.value.collect {
                        [
                                value: it.term,
                                count: it.count
                        ]
                    },
            ]
        }
    }
}

@Validateable
class AutoCompleteCommand {
    String requiredField
    String category
    String term

    static constraints = {
        requiredField blank: false
        category blank: false
        term blank: false
    }
}

@Validateable
class FieldTerms {
    String operator
    List<SearchTerm> searchTerms = []

    static constraints = {
        operator inList: ['OR', 'AND']
    }
}

@Validateable
class GetFacetsCommand {
    String requiredField
    String operator
    @BindUsing({ obj, source ->
        source['fieldTerms'].collectEntries { k, v ->
            [k, new FieldTerms(v).with { ft ->
                ft.searchTerms = ft.searchTerms.collect {
                    new SearchTerm(it)
                }
                ft
            }]
        }
    })
    Map<String, FieldTerms> fieldTerms

    static constraints = {
        operator inList: ['OR', 'AND']
        fieldTerms validator: { val, obj ->
            val.values().every { it.validate() }
        }
    }
}

@Validateable
class SearchTerm {
    String literalTerm
    String luceneTerm
}
