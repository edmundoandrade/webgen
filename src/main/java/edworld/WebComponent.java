// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebComponent {
	protected static final String COMPONENT_REGEX = ".*\\{([^\\}\\s]+)([^\\}]*)\\}(.*)";
	protected static final String PARAMETER_DELIMITER_REGEX = "\\|";
	protected String type;
	protected String title;
	protected String[] parameters;

	public WebComponent(String line) {
		Matcher matcher = Pattern.compile(COMPONENT_REGEX).matcher(line);
		if (matcher.find()) {
			type = matcher.group(1).trim();
			title = matcher.group(2).trim();
			parameters = extractParameters(matcher.group(3).trim());
		}
	}

	public static boolean matches(String line) {
		return line.matches(COMPONENT_REGEX);
	}

	public String getType() {
		return type;
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
