// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class WebComponent {
	protected static final String COMPONENT_REGEX = ".*\\{([^\\}]+)\\}(.*)";
	protected static final String PARAMETER_DELIMITER_REGEX = "\\|";
	protected String type;
	protected Document xmlData;
	protected Map<String, String> replacements = new HashMap<String, String>();;
	protected String title;
	protected String[] parameters;

	public WebComponent(String line) {
		Matcher matcher = Pattern.compile(COMPONENT_REGEX).matcher(line);
		if (matcher.find()) {
			String[] parts = matcher.group(1).trim().split("[\\(\\)]");
			if (parts.length > 1) {
				type = parts[0];
				title = parts[parts.length - 1].trim();
			} else {
				parts = matcher.group(1).trim().split("\\s", 2);
				type = parts[0];
				title = parts.length < 2 ? "" : parts[1];
			}
			if (parts.length > 1 && parts[1].startsWith("XML="))
				try {
					xmlData = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(parts[1].substring(4));
					parts[1] = "";
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}
			for (int i = 1; i < parts.length; i++)
				if (parts[i].contains("=")) {
					String[] definitionParts = parts[i].split("=");
					replacements.put(definitionParts[0].trim(), definitionParts[1].trim());
				}
			parameters = extractParameters(matcher.group(2).trim());
		} else {
			type = "_default";
			title = "";
			parameters = extractParameters(removeInitialWikiMarkers(line));
		}
	}

	private String removeInitialWikiMarkers(String line) {
		int start = 0;
		while (start < line.length() && "*# \t".contains(line.subSequence(start, start + 1)))
			start++;
		return line.substring(start);
	}

	public String getType() {
		return type;
	}

	public Document getXmlData() {
		return xmlData;
	}

	public Map<String, String> getReplacements() {
		return replacements;
	}

	public String getTitle() {
		return title;
	}

	protected String[] extractParameters(String info) {
		if (info.isEmpty())
			return new String[0];
		String[] result = info.split(PARAMETER_DELIMITER_REGEX);
		for (int i = 0; i < result.length; i++)
			result[i] = result[i].trim();
		return result;
	}

	public String[] getParameters() {
		return parameters;
	}
}
