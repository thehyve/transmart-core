/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import groovy.transform.CompileStatic
import groovy.transform.Immutable
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

    @Immutable
    static class Config {
        boolean dropIndexes
        boolean unlogged
        boolean write
        boolean temporaryTable
        String outputFile
        int batchSize
        int flushSize
    }

    static Options options = new Options()
    static {
        options.addOption('h', 'help', false, 'Help.')
        options.addOption('a', 'admin', false, 'Connect to the database as admin.')
        options.addOption('D', 'delete', true, 'Delete study by id.')
        options.addOption('r', 'restore-indexes', false, 'Restore indexes.')
        options.addOption('v', 'vacuum-analyze', false, 'Run vacuum analyze on the database.')
        options.addOption('i', 'drop-indexes', false, 'Drop indexes when loading.')
        options.addOption('u', 'unlogged', false, 'Set observations table to unlogged when loading.')
        options.addOption('t', 'temporary-table', false, 'Use a temporary table when loading.')
        options.addOption('b', 'batch-size', true, 'Number of observation to insert in a batch (default: 500).')
        options.addOption('f', 'flush-size', true, 'Number of batches to flush to the database (default: 1000).')
        options.addOption('w', 'write', true, 'Write observations to TSV file.')
        options.addOption('d', 'directory', true, 'Specifies a data directory.')
        options.addOption('m', 'mode', true, 'Load mode (e.g. \'study\' or \'pedigree\').')
    }

    static printHelp() {
        String header = 'Copy tool for loading TranSMART data into a PostgreSQL database.\n\n'
        String footer = '\nPlease report issues at https://github.com/thehyve/transmart-core/issues.'

        HelpFormatter formatter = new HelpFormatter()
        formatter.printHelp('transmart-copy', header, options, footer, true)
    }

    Database database
    Patients patients
    Studies studies

    void init(boolean connectAsAdmin) {
        database = new Database(connectAsAdmin)
    }

    void restoreIndexes() {
        def config = new Config(dropIndexes: true, unlogged: true)
        def observations = new Observations(database, null, null, null, config)
        observations.restoreTableIndexes()
    }

    void uploadStudy(String rootPath, Config config) {

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

        def observations = new Observations(database, studies, concepts, patients, config)
        observations.checkFiles(rootPath)
        observations.load(rootPath)
    }

    void uploadPedigree(String rootPath, Config config) {

        patients = new Patients(database)
        patients.fetch()
        patients.load(rootPath)

        def relations = new Relations(database, patients, config)
        relations.fetch()
        relations.load(rootPath)
    }

    void deleteStudy(String studyId) {
        log.info "Deleting study ${studyId} ..."
        def dimensions = new Dimensions(database)
        studies = new Studies(database, dimensions)
        studies.delete(studyId)
        log.info "Study ${studyId} deleted."
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
            copy.init(cl.hasOption('admin'))
            if (cl.hasOption('delete')) {
                def studyId = cl.getOptionValue('delete')
                try {
                    copy.deleteStudy(studyId)
                } catch(Exception e) {
                    log.error "Error deleting study ${studyId}."
                    throw e
                }
            } else if (cl.hasOption('restore-indexes')) {
                copy.restoreIndexes()
            } else {
                int batchSize = cl.hasOption('batch-size') ? cl.getOptionValue('batch-size') as int : Database.defaultBatchSize
                int flushSize = cl.hasOption('flush-size') ? cl.getOptionValue('flush-size') as int : Database.defaultFlushSize
                def config = new Config(
                        dropIndexes: cl.hasOption('drop-indexes'),
                        unlogged: cl.hasOption('unlogged'),
                        temporaryTable: cl.hasOption('temporary-table'),
                        batchSize: batchSize,
                        flushSize: flushSize,
                        write: cl.hasOption('write'),
                        outputFile: cl.getOptionValue('write')
                )
                String directory = cl.hasOption('directory') ? cl.getOptionValue('directory') : '.'
                if (cl.hasOption('mode')) {
                    def modes = cl.getOptionValues('mode')
                    log.debug("Load modes specified: ${modes}")
                    if ('pedigree' in modes) {
                        copy.uploadPedigree(directory, config)
                    }
                    if ('study' in modes) {
                        copy.uploadStudy(directory, config)
                    }
                } else {
                    log.debug('No load mode specified. Use default study mode.')
                    copy.uploadStudy(directory, config)
                }
            }
            if (cl.hasOption('vacuum-analyze')) {
                copy.database.vacuumAnalyze()
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
