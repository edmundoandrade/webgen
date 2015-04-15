// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class WebComponent {
	protected static final String COMPONENT_REGEX = ".*\\{([^\\}\\s]+)([^\\}]*)\\}(.*)";
	protected static final String PARAMETER_DELIMITER_REGEX = "\\|";
	protected String type;
	protected Document xmlData;
	protected String title;
	protected String[] parameters;

	public WebComponent(String line) {
		Matcher matcher = Pattern.compile(COMPONENT_REGEX).matcher(line);
		if (matcher.find()) {
			String[] parts = matcher.group(1).trim().split("[(=)]", 4);
			type = parts[0];
			if (parts.length > 2 && parts[1].equals("XML"))
				try {
					xmlData = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(parts[2]);
				} catch (Exception e) {
					throw new IllegalArgumentException(e);
				}

			title = matcher.group(2).trim();
			parameters = extractParameters(matcher.group(3).trim());
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
