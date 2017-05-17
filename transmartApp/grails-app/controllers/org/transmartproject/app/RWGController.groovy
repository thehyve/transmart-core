package org.transmartproject.app

import com.google.common.collect.ImmutableMap
import grails.databinding.BindUsing
import grails.validation.Validateable
import groovy.time.TimeCategory
import org.json.JSONObject
import org.transmart.biomart.Experiment
import org.transmart.searchapp.AccessLog
import org.transmart.searchapp.AuthUser
import org.transmartproject.browse.fm.FmFile
import org.transmartproject.browse.fm.FmFolder
import org.transmartproject.core.exceptions.InvalidRequestException

class RWGController {
    def trialQueryService
    def searchKeywordService
    def springSecurityService
    def formLayoutService
    def fmFolderService

    private static final Map<String, Integer> FOLDER_TYPE_COUNTS_EMPTY_TEMPLATE = ImmutableMap.of(
            'PROGRAM',  0,
            'STUDY',    0,
            'ASSAY',    0,
            'ANALYSIS', 0,
            'FOLDER',   0,)


    def index = {

        def exportList = session['export'];

        return [exportCount: exportList?.size(), debug: params.debug];
    }

    def ajaxWelcome = {
        //add a unused model to be able to use the template
        render(template: 'welcome', model: [page: "RWG"]);
    }

    def renderRoot(RenderRootCommand command) {
        if (!command.validate()) {
            throw new InvalidRequestException('bad parameters: ' + command.errors)
        }

        def user = AuthUser.findByUsername(springSecurityService.principal.username)
        def folderContentsAccessLevelMap = fmFolderService.getFolderContentsWithAccessLevelInfo(user, null)

        if (command.search && !command.folderIds) {
            render template: '/fmFolder/noResults',
                    plugin: 'folderManagement',
                    model: [resultNumber: new JSONObject(FOLDER_TYPE_COUNTS_EMPTY_TEMPLATE)]
            return
        }

        String folderSearchString
        String uniqueLeavesString
        def counts = [*: FOLDER_TYPE_COUNTS_EMPTY_TEMPLATE,]

        if (command.folderIds) {
            List<FmFolder> folders = FmFolder.findAllByIdInList(command.folderIds)
            folderSearchString = folders*.folderFullName.join(',') + ','
            // inefficient quadratic algorithm, but it shouldn't matter
            uniqueLeavesString = folders.findAll { folderUnderConsideration ->
                // false that it has a parent in the list
                !folders.any {
                    !it.is(folderUnderConsideration) &&
                            folderUnderConsideration.folderFullName.startsWith(it.folderFullName)
                }
            }*.folderFullName.join(',') + ','

            counts = [
                    *: counts,
                    *: folders.inject([:], { cur, el ->
                        cur[el.folderType] = (cur[el.folderType] ?: 0) + 1
                        cur
                    })]
        }

        render(
                template: '/fmFolder/folders',
                plugin: 'folderManagement',
                model: [
                        folderContentsAccessLevelMap: folderContentsAccessLevelMap,
                        folderSearchString: folderSearchString,
                        uniqueLeavesString: uniqueLeavesString,
                        auto: true,
                        resultNumber: new JSONObject(counts),
                        nodesToExpand: [],
                        nodesToClose:  [],
                ])
    }

    // Load the search results for the given search terms using the new annotation tables
    // return the html string to be rendered for the results panel
    @Deprecated // still used for GWAS; we need to investigate
    def loadSearchResults(studyCounts, startTime) {
        def exprimentAnalysis = [:]                        // Map of the trial objects and the number of analysis per trial
        def total = 0                                // Running total of analysis to show in the top banner

        def studyWithResultsFound = false

        for (studyId in studyCounts.keys().sort()) {
            def c = studyCounts[studyId].toInteger()

            if (c > 0) {
                studyWithResultsFound = true

                Long expNumber = Long.parseLong(studyId)

                def exp = Experiment.createCriteria()
                def experiment = exp.get {
                    eq("id", expNumber)
                }
                if (experiment == null) {
                    log.warn "Unable to find an experiment for ${expNumber}"
                } else {
                    exprimentAnalysis.put((experiment), c)
                    total += c
                }
            }
        }
        // capture html as a string that will be passed back in JSON object
        def html
        if (!studyWithResultsFound) {
            html = g.render(template: '/search/noResult').toString()
        } else {
            html = g.render(template: '/RWG/experiments', model: [experiments: exprimentAnalysis, analysisCount: total, duration: TimeCategory.minus(new Date(), startTime)]).toString()
        }

        return html
    }

    // Load the trial analysis for the given trial
    def getTrialAnalysis = {
        new AccessLog(username: springSecurityService.getPrincipal().username,
                event: "Loading trial analysis", eventmessage: params.trialNumber, accesstime: new Date()).save()

        def analysisList = trialQueryService.querySOLRTrialAnalysis(params, session.solrSearchFilter)
        render(template: '/RWG/analysis', model: [aList: analysisList])
    }

    def getFileDetails = {
        def layout = formLayoutService.getLayout('file')
        render(template: '/fmFolder/fileMetadata', model: [layout: layout, file: FmFile.get(params.id)])
    }
}


class RenderRootCommand implements Validateable {
    boolean search
    @BindUsing({ obj, source ->
        def folderIds = source['folderIds']
        if (!folderIds) {
            return []
        }
        if (!(folderIds instanceof List ) && !folderIds.getClass().array) {
            folderIds = [folderIds]
        }
        folderIds.collect {
            if (it.isLong()) {
                it as Long
            }
        }
    })
    List<Long> folderIds

    static constraints = {
        folderIds validator: { val, obj ->
            if (!obj.search && val) {
                return false
            }
            val.every { it instanceof Long }
        }
    }
}
