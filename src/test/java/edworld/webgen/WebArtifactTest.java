// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class WebArtifactTest {
	@Test(expected = IllegalArgumentException.class)
	public void getInvalidTemplate() throws IOException {
		WebArtifact.getTemplate("invalid-template", new File("target/web-templates"));
	}
}
