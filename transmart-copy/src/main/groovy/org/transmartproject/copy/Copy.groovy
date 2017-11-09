/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import org.transmartproject.copy.table.Concepts
import org.transmartproject.copy.table.Dimensions
import org.transmartproject.copy.table.Modifiers
import org.transmartproject.copy.table.Observations
import org.transmartproject.copy.table.Patients
import org.transmartproject.copy.table.Relations
import org.transmartproject.copy.table.Studies
import org.transmartproject.copy.table.Tags
import org.transmartproject.copy.table.TreeNodes

/**
 * Command-line utility to copy tab delimited files to the TranSMART database.
 *
 * @author gijs@thehyve.nl
 */
@Slf4j
@CompileStatic
class Copy {

    static Options options = new Options()
    static {
        options.addOption('h', 'help', false, 'Help.')
        options.addOption('d', 'delete', true, 'Delete study by id.')
    }

    static printHelp() {
        String header = 'Copy tool for loading TranSMART data into a PostgreSQL database.\n\n'
        String footer = '\nPlease report issues at https://github.com/thehyve/transmart-core/issues.'

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp('transmart-copy', header, options, footer, true)
    }

    Database database
    Patients patients
    Studies studies

    void init() {
        database = new Database()
        database.init()
    }

    void run(String rootPath) {
        File.createTempDir('transmart-copy-', '-tmp').with { tmpDir ->

            // Check if dimensions are present, load mapping from file
            def dimensions = new Dimensions(database)
            dimensions.fetch()
            dimensions.load(rootPath)

            // Check if study is not already present
            studies = new Studies(database, dimensions)
            studies.check(rootPath)
            // Insert study, trial visit objects
            studies.load(rootPath)

            patients = new Patients(database)
            patients.fetch()
            patients.load(rootPath)

            def relations = new Relations(database, patients)
            relations.fetch()
            relations.load(rootPath, tmpDir)

            def concepts = new Concepts(database)
            concepts.fetch()
            concepts.load(rootPath)

            def modifiers = new Modifiers(database)
            modifiers.fetch()
            modifiers.load(rootPath)

            def treeNodes = new TreeNodes(database, studies, concepts)
            treeNodes.fetch()
            treeNodes.load(rootPath)

            def tags = new Tags(database, treeNodes)
            tags.fetch()
            tags.load(rootPath)

            def observations = new Observations(database, studies, concepts, patients)
            observations.checkFiles(rootPath)
            observations.load(rootPath, tmpDir)

            database.vacuumAnalyze()
        }
    }

    void deleteStudy(String studyId) {
        log.info "Deleting study ${studyId} ..."
        def dimensions = new Dimensions(database)
        studies = new Studies(database, dimensions)
        studies.delete(studyId)
        log.info "Study ${studyId} deleted."

        database.vacuumAnalyze()
    }

    static void main(String[] args) {
        def parser = new DefaultParser()
        try {
            CommandLine cl = parser.parse(options, args)
            if (cl.hasOption('help')) {
                printHelp()
                return
            }
            def copy = new Copy()
            copy.init()
            if (cl.hasOption('delete')) {
                def studyId = cl.getOptionValue('delete')
                try {
                    copy.deleteStudy(studyId)
                } catch(Exception e) {
                    log.error "Error deleting study ${studyId}"
                }
            } else {
                copy.run('.')
            }
        } catch (ParseException e) {
            log.error e.message
            println()
            printHelp()
            System.exit(2)
        } catch (Exception e) {
            log.error e.message
            System.exit(1)
        }
    }

}
