// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import org.junit.Assert;
import org.junit.Test;

import edworld.util.TextUtil;

public class TextUtilTest {
	@Test
	public void standardId() {
		TextUtil textUtil = new TextUtil();
		Assert.assertEquals("a_complex_name_title", textUtil.standardId("a complex\\name/title?."));
		Assert.assertEquals("date_time", textUtil.standardId(":Date/Time;"));
		Assert.assertEquals("ab", textUtil.standardId("a.b"));
		Assert.assertEquals("_", textUtil.standardId("."));
	}
}
