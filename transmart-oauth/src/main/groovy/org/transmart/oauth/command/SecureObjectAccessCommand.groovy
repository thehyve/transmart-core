/**
 * @author JIsikoff
 *
 */
package org.transmart.oauth.command

import grails.validation.Validateable

class SecureObjectAccessCommand implements Validateable {
    String[] sobjectstoadd
    String[] sobjectstoremove
    String[] groupstoadd
    String[] groupstoremove
    String accesslevelid
    String searchtext
}
