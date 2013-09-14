/* Clone the pluginsʼ corresponding repository alongside transmartApp and
 * uncomment the settings below in order to use those plugins inline.  See
 * http://grails.org/doc/latest/guide/plugins.html#12.1%20Creating%20and%20Installing%20Plug-ins
 * under the heading ‘Specifying Plugin Locations’ for more information. */

//grails.plugin.location.'rdc-rmodules'     = '../Rmodules'
//grails.plugin.location.'transmart-core'   = '../core-db'
//grails.plugin.location.'dalliance-plugin' = '../dalliance-plugin'
//grails.plugin.location.'transmart-mydas'  = '../transmart-mydas'

grails.project.dependency.resolution = {
    repositories {
        mavenRepo([
                name: 'repo.thehyve.nl-public',
                url: 'https://repo.thehyve.nl/content/repositories/public/',
        ])

        /* If you set the grails.project.dependency.resolution config property,
         * then the tranSMART Foundation repository will not be used (see the
         * in-tree BuildConfig.groovy). You can re-add it by uncommenting the
         * block below. It will have lower priority than the repositories listed
         * in BuildConfig.groovy (which have the highest priority) and the
         * repositories that are listed above it in this closure. */
//        mavenRepo([
//                name: 'repo.transmartfoundation.org-public',
//                root: 'https://repo.transmartfoundation.org/content/repositories/public/',
//        ])
    }

}

// vim: filetype=groovy et ts=4 sts=4 sw=4 tw=80:
