/* (c) Copyright 2017, tranSMART Foundation, Inc. */

package org.transmartproject.rest

import com.google.common.collect.ImmutableMap
import groovy.transform.Immutable
import org.springframework.web.bind.annotation.PathVariable
import org.transmartproject.core.exceptions.NoSuchResourceException
import org.transmartproject.rest.marshallers.ContainerResponseWrapper

class VersionController {

    static responseFormats = ['json', 'hal']

    /*
     * How versioning works:
     *
     * The rest api can support multiple major versions at a time (currently v1 and v2). They have separate prefixes
     * in their urls. Each major version has a specific version number of the form major.minor(.patch)(-tag). The
     * version numbering scheme follows <a href="http://semver.org/">Semantic Versioning</a>.
     *
     * Compatible changes in the api are indicated by their minor version number. Each group of changes made to the
     * api must increase the minor version number.
     *
     * The patch number is currently not used, but could be used to indicate bugfixes that are important to know,
     * e.g. if security bugs need to be fixed.
     *
     * To accommodate parralel development branches, there is also a list of features. Each feature is a name and a
     * feature version number, which indicate the availability of a certain feature and optinally a revision. The
     * current code automatically sets all feature numbers to 1 as we haven't had to use feature revisions yet, but
     * this can be changed if needed.
     *
     * Features are only allowed on development versions of the api, indicated by a -dev tag. Once a set of features
     * is ready, usually that will be when a project is completed, or when a new version of Transmart is released,
     * the -dev tag and list of features should be removed. New development should increment the version number and
     * add the -dev tag back in.
     *
     * Note that e.g. version 2.1-dev indicates development for version 2.1. So version 2.1 is newer than 2.1-dev.
     * Version numbering will usually proceed as 2.0 -> 2.1-dev -> 2.1 -> 2.2-dev -> 2.2, etc.
     *
     * The JSON for a version looks like
     *     {
     *       'id': 'v2',
     *       'prefix': '/v2',
     *       'version': '2.1',
     *       'major': 2,
     *       'minor': 1,
     *       'tag': 'dev',
     *       'features': {
     *           'versioning': 1
     *       }
     *     }
     *
     * 'minor', 'patch', and 'features' keys are optional.
     *
     * The JSON for all versions is a map with version ids as keys and maps as above as values.
     */

    public static final ImmutableMap<String, VersionWrapper> versions = makeVersions([
            [
                id: 'v1',
                version: '1.1'
            ],[
                id: 'v2',
                version: '2.5',
                features: [
                        'versioning',
                        'and-or-constraints',
                        'values_aggregate',
                        'inline-selections',
                        'observation-sets',
                        'pedigree',
                        'survey-table-view',
                        'query-subscription',
                        'crosstable',
                        'after_data_loading_update',
                        'aggregates_per_numerical_and_categorical_concept',
                        'counts_with_threshold'
                ]
            ]
    ])

    /**
     * The current version as string for the major version id
     * @param id, e.g. 'v2'
     * @return the full version, e.g. '2.1-dev'
     */
    static String currentVersion(String id) {
        versions[id].version.version
    }

    // Make everything immutable
    static ImmutableMap makeVersions(List args) {
        ImmutableMap.copyOf(args.collectEntries { Map it ->
            it += splitVersion(it.version)
            it.prefix = "/${it.id}".toString()

            // Feature tags are only allowed for -dev versions (or other such tagged versions)
            if(it.tag && it.features) {
                // All tags have a feature version of 1 for the moment, feel free to change this if there is a need
                it.features = ImmutableMap.copyOf(it.features.collectEntries { String feature ->
                    [feature, 1]
                })
            } else {
                it.remove('features')
            }

            [it.id, new VersionWrapper(ImmutableMap.copyOf(it))]
        })
    }

    static splitVersion(String versionNumber) {
        // major.minor.patch-tag
        // patch and tag are optional
        def m = versionNumber =~ /(\d+)\.(\d+)(\.(\d+))?(-(\w+))?/
        assert m.matches()
        return [
                major: m.group(1) as int,
                minor: m.group(2) as int,
                patch: m.group(4) as Integer,
                tag: m.group(6),
        ].findAll { k, v -> v != null }
    }

    def index() {
        respond new ContainerResponseWrapper(
                key: 'versions',
                container: versions,
                componentType: Map,
        )
    }

    def show(@PathVariable('id') String id) {
        VersionWrapper version = versions[id]
        if (version == null) {
            throw new NoSuchResourceException("Version not available: ${id}.")
        }
        respond version
    }

    @Immutable(knownImmutableClasses = [ImmutableMap])
    static class VersionWrapper {
        ImmutableMap version
    }
}

