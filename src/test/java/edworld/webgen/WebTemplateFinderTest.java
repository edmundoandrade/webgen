// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class WebTemplateFinderTest {
	private WebTemplateFinder templateFinder;

	@Before
	public void setUp() {
		templateFinder = new WebTemplateFinder(new File("target/web-templates"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void getInvalidTemplate() throws IOException {
		templateFinder.getTemplate("invalid-template", null);
	}

	@Test
	public void getTemplateWithReplacements() {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("description", "This is a description.");
		replacements.put("customSelectClasses", "");
		replacements.put("customLabelClasses", "");
		Assert.assertEquals(
				"<label title=\"This is a description.\" class=\"form-group \">${title}<select id=\"${id}\" class=\"\">${data:select-item}</select></label>",
				templateFinder.getTemplate("select", replacements));
	}
}
