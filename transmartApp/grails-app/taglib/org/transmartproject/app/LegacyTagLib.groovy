package org.transmartproject.app

class LegacyTagLib {

    static namespace = 'legacy'

    static defaultEncodeAs = [taglib: 'raw']

    /**
     * Taglib function meant to replace the deprecated (and now removed)
     * <code>remoteFunction</code> tag lib function from grails.
     * It is not supposed to be a full replacement, but just to do what is
     * needed to keep transmartApp functioning.
     * Already deprecated from the start.
     *
     * Example:
     * <code>
     * ${remoteFunction(action: 'searchGroupsWithoutUser', update: [success: 'groups', failure: ''], params: 'jQuery(\'#searchtext\').serialize()+\'&id=\'+pid')};
     * </code>
     * Result:
     * <code>
     * jQuery.ajax({
     *     type:'POST',
     *     data: jQuery('#searchtext').serialize()+'&id='+pid,
     *     url: '/transmart/userGroup/searchGroupsWithoutUser',
     *     success: function(data, textStatus) {
     *         jQuery('#groups').html(data);
     *     },
     *     error: function(XMLHttpRequest, textStatus, errorThrown){
     *
     *     }
     * });
     * return false;
     }
     * </code>
     */
    @Deprecated
    def remoteFunction = { attrs ->
        def updateElem = null
        def successElem
        def failureElem
        def successHandler = attrs.onSuccess
        def failureHandler = attrs.onFailure

        if (attrs.update instanceof Map) {
            successElem = attrs.update?.success
            failureElem = attrs.update?.failure

        } else {
            updateElem = attrs.update

        }
        def queryParams = attrs.params as String
        def urlParams = ['controller', 'action', 'id'].findAll {
            attrs.containsKey(it)
        }.collectEntries {
            [(it): attrs[it]]
        }
        def before = attrs.before ?: ''
        def onComplete = attrs.onComplete ?: ''

        out << """jQuery.ajax({
    type: 'POST',
    data: ${queryParams},
    url: '${createLink(urlParams)}',\n"""
        if (before) {
            out << "    beforeSend: function(XMLHttpRequest, settings) { ${before} },\n"
        }
        if (onComplete) {
            out << "    complete: function(XMLHttpRequest, textStatus) { ${onComplete} },\n"
        }
        out << """    success: function(data, textStatus) {
        ${ successHandler ? successHandler + ';' : '' }
        ${ updateElem ? "jQuery('#${updateElem}').val(data);" : '' }
        ${ successElem ? "jQuery('#${successElem}').html(data);" : '' }
    },
    error: function(XMLHttpRequest, textStatus, errorThrown) {
        ${ failureHandler ? failureHandler + ';' : '' }
        ${ failureElem ? "jQuery('#${failureElem}').html(textStatus);" : '' }
    }\n"""
         out << '});\n'
    }

}
