/**
 * @author JIsikoff
 *
 */
package org.transmart.oauth.command

import grails.validation.Validateable

class UserGroupCommand implements Validateable {
    String[] userstoadd
    String[] userstoremove
    String[] groupstoadd
    String[] groupstoremove
    String searchtext
}
