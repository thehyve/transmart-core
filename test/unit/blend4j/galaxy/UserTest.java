package blend4j.galaxy;

import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import com.github.jmchilton.blend4j.galaxy.UsersClient;
import com.github.jmchilton.blend4j.galaxy.beans.User;
import com.github.jmchilton.blend4j.galaxy.beans.UserCreate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.UUID;

public class UserTest {
  private GalaxyInstance instance;
  private UsersClient client;

  @BeforeMethod
  public void init() {
    instance = TestGalaxyInstance.get();
    client = instance.getUsersClient();
  }
  
  @Test
  public void testUserCreate() {
    final UserCreate userCreate = new UserCreate();
    final String username = UUID.randomUUID().toString();
    final String email = username + "@example.com";
    userCreate.setEmail(email);
    userCreate.setUsername(username);
    userCreate.setPassword("testpass");
    
    final User user = client.createUser(userCreate);
    assert user.getEmail().equals(email);
    assert user.getUsername().equals(username);
    
    final String apiKey = client.createApiKey(user.getId());
    System.out.println(apiKey);
  }

}
