package blend4j.plugin

import grails.transaction.Transactional

@Transactional
class GalaxyUserDetailsService {

        def saveNewGalaxyUser(String username, String galaxyKey, String mailAddress) {
            try{
                def galaxyUser = new GalaxyUserDetails()
                galaxyUser.username = username;
                galaxyUser.galaxyKey = galaxyKey;
                galaxyUser.mailAddress = mailAddress;
                System.err.println("I passed here")
                System.err.println(username)
                System.err.println(galaxyKey)
                System.err.println(mailAddress)
                galaxyUser.save();
                System.err.println("I passed teh save")
            }catch(e){
                log.error("The export job for galaxy couldn't be saved")
                return false;
            }
            return true;
        }

}
