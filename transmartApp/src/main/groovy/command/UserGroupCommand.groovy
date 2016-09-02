/**
 * @author JIsikoff
 *
 */
package command

public class UserGroupCommand implements grails.validation.Validateable {
    String[] userstoadd
    String[] userstoremove
    String[] groupstoadd
    String[] groupstoremove
    String searchtext
}
