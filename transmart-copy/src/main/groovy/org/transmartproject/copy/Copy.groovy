/*
 * Copyright (c) 2017  The Hyve B.V.
 *  This file is distributed under the GNU General Public License
 *  (see accompanying file LICENSE).
 */

package org.transmartproject.copy

import groovy.transform.CompileStatic
import groovy.transform.Immutable
import groovy.util.logging.Slf4j
import org.apache.commons.cli.*
import org.transmartproject.copy.table.*

/**
 * Command-line utility to copy tab delimited files to the TranSMART database.
 *
 * @author gijs@thehyve.nl
 */
@Slf4j
@CompileStatic
class Copy implements AutoCloseable {

    @Immutable
    static class Config {
        boolean write
        String outputFile
        int batchSize
        int flushSize
        boolean updateConceptPaths
        boolean partition
        boolean unlogged
        boolean baseOnMaxInstanceNum
    }

    static Options options = new Options()
    static {
        options.addOption('h', 'help', false,
                'Help.')
        options.addOption('D', 'delete', true,
                'Delete study by id.')
        options.addOption('r', 'restore-indexes', false,
                'Restore indexes.')
        options.addOption('v', 'vacuum-analyze', false,
                'Run vacuum analyze on the database.')
        options.addOption('i', 'drop-indexes', false,
                'Drop indexes when loading.')
        options.addOption('u', 'unlogged', false,
                'Set observations table to unlogged when loading.')
        options.addOption('b', 'batch-size', true,
                'Number of observation to insert in a batch (default: 500).')
        options.addOption('f', 'flush-size', true,
                'Number of batches to flush to the database (default: 1000).')
        options.addOption('w', 'write', true,
                'Write observations to TSV file.')
        options.addOption('d', 'directory', true,
                'Specifies a data directory.')
        options.addOption('m', 'mode', true,
                'Load mode (e.g. \'study\' or \'pedigree\').')
        options.addOption('U', 'update-concept-paths', false,
                'Updates concept paths and tree nodes when there is concept code collision.')
        options.addOption('p', 'partition', false,
                'Partition observation_fact table based on trial_visit_num.')
        options.addOption('n', 'base-on-max-instance-num', false,
                'Adds to each instance num a base. The base detected as max(observation_fact.incstance_num).')
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

    Copy(Map<String, String> params) {
        database = new Database(params)
    }

    void restoreIndexes() {
        def observations = new Observations(database, null, null, null, null)
        observations.restoreTableIndexesIfNotExist()
    }

    void dropIndexes() {
        def observations = new Observations(database, null, null, null, null)
        observations.dropTableIndexesIfExist()
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

        def concepts = new Concepts(database, config.updateConceptPaths)
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

    void deleteStudyById(String studyId, boolean failOnNoStudy = true) {
        log.info "Deleting study ${studyId} ..."
        def dimensions = new Dimensions(database)
        studies = new Studies(database, dimensions)
        studies.deleteById(studyId, failOnNoStudy)
        log.info "Study ${studyId} deleted."
    }

    void deleteStudy(String rootPath, boolean failOnNoStudy = true) {
        log.info "Deleting studies found in ${rootPath} ..."
        def dimensions = new Dimensions(database)
        studies = new Studies(database, dimensions)
        studies.delete(rootPath, failOnNoStudy)
        log.info "Study found in ${rootPath} deleted."
    }

    static void main(String[] args) {
        def parser = new DefaultParser()
        try {
            CommandLine cl = parser.parse(options, args)
            if (!cl.options) {
                printHelp()
                System.exit(1)
            }
            if (cl.hasOption('help')) {
                printHelp()
                System.exit(0)
            }
            runCopy(cl, [:].withDefault { Object key -> System.getenv((String) key) })
        } catch (ParseException e) {
            log.error e.message
            println()
            printHelp()
            System.exit(2)
        } catch (Throwable e) {
            log.error e.message, e
            System.exit(1)
        }
    }

    public final static Set<String> INDEPENDENT_OPERATION_OPTIONS = ['help', 'delete', 'drop-indexes',
                                                                     'restore-indexes', 'vacuum-analyze'] as Set

    static void runCopy(CommandLine cl, Map<String, String> params) {
        def copy = new Copy(params)
        def tx = copy.database.beginTransaction()
        try {
            if (cl.hasOption('delete')) {
                def studyId = cl.getOptionValue('delete')
                copy.deleteStudyById(studyId)
            }
            if (cl.hasOption('drop-indexes')) {
                copy.dropIndexes()
            }
            boolean independentOperationOptionSpecified = INDEPENDENT_OPERATION_OPTIONS
                    .any { String longOpt -> cl.hasOption(longOpt) }
            if (cl.hasOption('directory') || !independentOperationOptionSpecified) {
                String directory
                if (cl.hasOption('directory')) {
                    directory = cl.getOptionValue('directory')
                } else {
                    directory = '.'
                }
                int batchSize = cl.hasOption('batch-size') ?
                        cl.getOptionValue('batch-size') as int : Database.DEFAULT_BATCH_SIZE
                int flushSize = cl.hasOption('flush-size') ?
                        cl.getOptionValue('flush-size') as int : Database.DEFAULT_FLUSH_SIZE
                def config = new Config(
                        batchSize: batchSize,
                        flushSize: flushSize,
                        write: cl.hasOption('write'),
                        outputFile: cl.getOptionValue('write'),
                        updateConceptPaths: cl.hasOption('update-concept-paths'),
                        partition: cl.hasOption('partition'),
                        unlogged: cl.hasOption('unlogged'),
                        baseOnMaxInstanceNum: cl.hasOption('base-on-max-instance-num')
                )
                def modes = cl.getOptionValues('mode')
                log.debug("Load modes specified: ${modes}")
                if ('pedigree' in modes) {
                    copy.uploadPedigree(directory, config)
                }
                if (!modes || 'study' in modes) {
                    copy.deleteStudy(directory, false)
                    copy.uploadStudy(directory, config)
                }
            }
            if (cl.hasOption('restore-indexes')) {
                copy.restoreIndexes()
            }
            copy.database.commit(tx)
            if (cl.hasOption('vacuum-analyze')) {
                copy.database.vacuumAnalyze()
            }
        } catch (Throwable e) {
            copy.database.rollback(tx)
            throw e
        }
    }

    @Override
    void close() throws Exception {
        database.close()
    }

}
