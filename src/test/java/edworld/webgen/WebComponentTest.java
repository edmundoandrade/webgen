// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import org.junit.Assert;
import org.junit.Test;

public class WebComponentTest {
	@Test
	public void toWebComponent() {
		Assert.assertNull(WebComponent.toWebComponent("Title"));
		WebComponent component = WebComponent.toWebComponent("((class=text-right) TCab3)");
		Assert.assertEquals("TCab3", component.getTitle());
		Assert.assertEquals("text-right", component.getReplacements().get("class"));
	}
}
