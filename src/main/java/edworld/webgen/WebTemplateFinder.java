// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;

import edworld.util.TextUtil;

public class WebTemplateFinder {
	protected static TextUtil textUtil = new TextUtil();

	protected File templatesDir;

	public WebTemplateFinder(File templatesDir) {
		this.templatesDir = templatesDir;
	}

	public String getTemplate(String templateName, Map<String, String> replacements) {
		return getTemplate(templateName, replacements, ".html");
	}

	public String getTemplate(String templateName, Map<String, String> replacements, String templateExtension) {
		String fileName = textUtil.standardId(templateName) + templateExtension;
		File templateFile = new File(templatesDir, fileName);
		if (templateFile.exists())
			try {
				return applyReplacements(textUtil.extractText(new FileInputStream(templateFile)), replacements);
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		String resourceName = "/templates/" + fileName;
		InputStream templateStream = streamFromResourceName(resourceName);
		if (templateStream == null)
			throw new IllegalArgumentException("Template resource not found: " + resourceName);
		return applyReplacements(textUtil.extractText(templateStream), replacements);
	}

	protected InputStream streamFromResourceName(String resourceName) {
		return WebTemplateFinder.class.getResourceAsStream(resourceName);
	}

	protected String applyReplacements(String text, Map<String, String> replacements) {
		if (replacements == null)
			return text;
		String result = text;
		for (String key : replacements.keySet())
			result = result.replaceAll("\\$\\{" + key + "\\}", Matcher.quoteReplacement(replacements.get(key)));
		return result;
	}
}
