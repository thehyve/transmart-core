package blend4j.galaxy;

import com.github.jmchilton.blend4j.galaxy.ConfigurationClient;
import com.github.jmchilton.blend4j.galaxy.GalaxyInstance;
import org.testng.annotations.Test;

import java.util.Map;

public class ConfigurationTest {

  @Test
  public void testPathPaste() {
    final GalaxyInstance galaxyInstance = TestGalaxyInstance.get();
    final ConfigurationClient client = galaxyInstance.getConfigurationClient();
    final Map<String, Object> config = client.getRawConfiguration();
    assert config.containsKey("terms_url");
  }

}
