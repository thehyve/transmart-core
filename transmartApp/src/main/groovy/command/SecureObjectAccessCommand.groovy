/**
 * @author JIsikoff
 *
 */
package command

public class SecureObjectAccessCommand implements grails.validation.Validateable {
    String[] sobjectstoadd
    String[] sobjectstoremove
    String[] groupstoadd
    String[] groupstoremove
    String accesslevelid
    String searchtext
}
