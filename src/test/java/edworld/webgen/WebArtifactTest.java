// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class WebArtifactTest {
	@Test(expected = IllegalArgumentException.class)
	public void getInvalidTemplate() throws IOException {
		WebArtifact.getTemplate("invalid-template", null, new File("target/web-templates"));
	}

	@Test
	public void getTemplateWithReplacements() {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("description", "This is a description.");
		replacements.put("customSelectClasses", "");
		replacements.put("customLabelClasses", "");
		Assert.assertEquals("<label title=\"This is a description.\" class=\"form-group \">${title}<select id=\"${id}\" class=\"\">${data:select-item}</select></label>",
				WebArtifact.getTemplate("select", replacements, new File("target/web-templates")));
	}

	@Test
	public void standardId() {
		Assert.assertEquals("a_complex_name_title", WebArtifact.standardId("a complex\\name/title?."));
		Assert.assertEquals("date_time", WebArtifact.standardId(":Date/Time;"));
	}
}
