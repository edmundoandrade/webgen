// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edworld.util.TextUtil;

public class WebArtifact {
	private static final String PARAMETER_SUFFIX_REGEX = "::.*";
	protected static final String CONTENT_PLACE = "${content}";
	protected static final String CONTENT_REGEX = "\\$\\{content\\}";
	protected static final String ITEM_TEMPLATE_REGEX = "\\$\\{data:([^\\}]+)\\}";
	protected static final String HEADER_TEMPLATE_REGEX = "\\$\\{header:([^\\}]+)\\}";
	protected static final String ATTRIBUTE_REGEX = "\\$\\{attribute:([^\\}]+)\\}";
	protected static final String ELEMENT_REGEX = "\\$\\{element:([^\\}]+)\\}";
	protected static final String ID_PLACE = "${id}";
	protected static final String NAME_PLACE = "${name}";
	protected static final String TITLE_PLACE = "${title}";
	protected static final String DEFAULT_DATA_CONTEXT = "default";
	protected static final String PROP_DESCRIPTION = "description";
	protected static final String PROP_PLACEHOLDER = "placeholder";
	protected static final String PROP_INPUT = "input";
	protected static final String LINE_BREAK = System.getProperty("line.separator");
	protected static TextUtil textUtil = new TextUtil();

	private String title;
	private String content;
	private String fileName;
	private int dataInputs;
	private int dataOutputs;
	protected Map<String, String> dataBehavior;
	protected Map<String, String> dataAlias;
	protected File templatesDir;
	protected Document data;
	protected Stack<String> parentContext = new Stack<String>();
	protected List<String> components = new ArrayList<String>();

	public WebArtifact(String title, String content, String fileName, Map<String, String> dataBehavior,
			Map<String, String> dataAlias, File templatesDir, Document data) {
		this.title = title;
		this.content = content;
		this.fileName = fileName;
		this.dataBehavior = dataBehavior;
		this.dataAlias = dataAlias;
		this.templatesDir = templatesDir;
		this.data = data;
		parentContext.push(CONTENT_PLACE);
	}

	public static String standardId(String context) {
		String result = textUtil.removeDiacritics(context.toLowerCase()).replaceAll("[ /\\\\]", "_")
				.replaceAll("[.,:;'\"?!]", "");
		return result.isEmpty() ? "_" : result;
	}

	public static String getTemplate(String templateName, Map<String, String> replacements, File templatesDir) {
		return getTemplate(templateName, replacements, ".html", templatesDir);
	}

	public static String getTemplate(String templateName, Map<String, String> replacements, String templateExtension,
			File templatesDir) {
		String fileName = standardId(templateName) + templateExtension;
		File templateFile = new File(templatesDir, fileName);
		if (templateFile.exists())
			try {
				return applyReplacements(textUtil.extractText(new FileInputStream(templateFile)), replacements);
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		InputStream templateStream = WebArtifact.class.getResourceAsStream("/templates/" + fileName);
		if (templateStream == null)
			throw new IllegalArgumentException("Template resource not found: " + fileName);
		return applyReplacements(textUtil.extractText(templateStream), replacements);
	}

	protected static String applyReplacements(String text, Map<String, String> replacements) {
		if (replacements == null)
			return text;
		String result = text;
		for (String key : replacements.keySet())
			result = result.replaceAll("\\$\\{" + key + "\\}", Matcher.quoteReplacement(replacements.get(key)));
		return result;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getFileName() {
		return fileName;
	}

	public int getDataInputs() {
		return dataInputs;
	}

	public void setDataInputs(int dataInputs) {
		this.dataInputs = dataInputs;
	}

	public int getDataOutputs() {
		return dataOutputs;
	}

	public void setDataOutputs(int dataOutputs) {
		this.dataOutputs = dataOutputs;
	}

	public void updateArtifact(String line) {
		updateLevel(line);
		String contentPlace = parentContext.peek();
		setContent(apply(component(line), getContent(), contentPlace));
	}

	private void updateLevel(String line) {
		int level = 0;
		int pos = 0;
		while (pos < line.length() && " *#".contains(line.substring(pos, pos + 1))) {
			if ("*#".contains(line.substring(pos, pos + 1)))
				level++;
			pos++;
		}
		while (level > 0 && parentContext.size() > level)
			parentContext.pop();
	}

	private String apply(String component, String parent, String place) {
		if (component.toLowerCase().contains("</button>"))
			dataInputs++;
		return parent.replace(place, component + place);
	}

	private String component(String line) {
		WebComponent component = new WebComponent(line);
		String id = createId(component);
		String contentPlace = pushContext(id);
		String content = fillMetaData(getTemplate(component.getType(), component.getReplacements(), templatesDir), id,
				component.getTitle());
		content = resolveHeader(id, component, content);
		content = resolveData(id, component, content);
		if (content.toLowerCase().contains("</form>") || content.toLowerCase().contains("</fieldset>"))
			content = content.replaceAll(CONTENT_REGEX,
					Matcher.quoteReplacement(generateInputFields(component.getParameters()) + contentPlace))
					+ LINE_BREAK;
		else
			content = content.replaceAll(CONTENT_REGEX,
					Matcher.quoteReplacement(generateGenericContent(component.getParameters()) + contentPlace))
					+ LINE_BREAK;
		return content;
	}

	private String resolveData(String id, WebComponent component, String content) {
		if (content.contains("${data}"))
			content = content.replaceAll("\\$\\{data\\}", Matcher.quoteReplacement(data(id, component)));
		Matcher matcher = Pattern.compile(ITEM_TEMPLATE_REGEX).matcher(content);
		while (matcher.find()) {
			String[] templates = matcher.group(1).split("@", 2);
			String rowTemplate = templates.length < 2 ? CONTENT_PLACE : getTemplate(templates[1], null, templatesDir);
			content = content.replaceAll("\\$\\{data:" + matcher.group(1) + "\\}",
					Matcher.quoteReplacement(buildComponentData(templates[0], id, component, rowTemplate)));
		}
		return content;
	}

	private String data(String dataId, WebComponent component) {
		dataOutputs++;
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			if (component.getXmlData() == null)
				return xpath.compile("//" + standardId(getTitle()) + "/" + dataId + "/text()").evaluate(data);
			else
				return xpath.compile("//" + dataId + "/text()").evaluate(component.getXmlData());
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private String resolveHeader(String id, WebComponent component, String content) {
		Matcher matcher = Pattern.compile(HEADER_TEMPLATE_REGEX).matcher(content);
		while (matcher.find()) {
			String[] templates = matcher.group(1).split("@", 2);
			String rowTemplate = templates.length < 2 ? CONTENT_PLACE : getTemplate(templates[1], null, templatesDir);
			content = content.replaceAll("\\$\\{header:" + matcher.group(1) + "\\}",
					Matcher.quoteReplacement(buildComponentHeader(templates[0], component, rowTemplate)));
		}
		return content;
	}

	private String generateInputFields(String[] fields) {
		String result = "";
		for (String field : fields)
			result += generateTextInput(field, getDescription(field), getPlaceHolder(field), "") + LINE_BREAK;
		return result;
	}

	private String generateTextInput(String field, String description, String placeHolder, String value) {
		dataInputs++;
		String title = field;
		String id = createId(title);
		WebComponent component = new WebComponent("{" + getInput(field) + " " + title + "}");
		String result = resolveData(id, component,
				fillMetaData(getTemplate(component.getType(), component.getReplacements(), templatesDir), quote(id),
						title).replaceAll("\\$\\{description\\}", quote(description))
								.replaceAll("\\$\\{placeholder\\}", quote(placeHolder))
								.replaceAll("\\$\\{value\\}", quote(value)));
		return removeVariablesNotReplaced(result);
	}

	private String generateGenericContent(String[] parameters) {
		String result = "";
		for (String parameter : parameters)
			result += parameter + LINE_BREAK;
		return result;
	}

	private String removeVariablesNotReplaced(String html) {
		return html.replaceAll("\\s?\\$\\{[^\\}]*\\}\\s?", "");
	}

	private String buildComponentHeader(String itemTemplate, WebComponent component, String rowTemplate) {
		String content = "";
		for (String parameter : component.getParameters())
			content += generateComponentItem(itemTemplate + parameterSuffix(parameter), parameterName(parameter));
		return rowTemplate.replaceAll(CONTENT_REGEX, Matcher.quoteReplacement(content));
	}

	private String parameterName(String parameter) {
		return parameter.replaceAll(PARAMETER_SUFFIX_REGEX, "");
	}

	private String parameterSuffix(String parameter) {
		Matcher matcher = Pattern.compile(PARAMETER_SUFFIX_REGEX).matcher(parameter);
		return matcher.find() ? matcher.group().replaceAll("::", "-") : "";
	}

	private String buildComponentData(String itemTemplate, String id, WebComponent component, String rowTemplate) {
		dataOutputs += Math.max(1, component.getParameters().length);
		if (data == null)
			return "";
		String content = "";
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			NodeList rows = dataRows(id, component, xpath);
			String separator = "";
			for (int i = 0; i < rows.getLength(); i++) {
				String rowContent = "";
				for (Node dataField : getDataFields(component.getParameters(), xpath, rows.item(i)))
					rowContent += generateComponentItem(itemTemplate, dataField);
				content += separator + rowTemplate.replaceAll(CONTENT_REGEX, Matcher.quoteReplacement(rowContent));
				separator = LINE_BREAK;
			}
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
		return content;
	}

	private Node[] getDataFields(String[] fields, XPath xpath, Node dataItem) throws XPathExpressionException {
		if (fields.length == 0)
			return new Node[] { dataItem };
		Node[] cells = new Node[fields.length];
		for (int i = 0; i < cells.length; i++)
			cells[i] = (Node) xpath.compile(standardId(parameterName(fields[i]))).evaluate(dataItem,
					XPathConstants.NODE);
		return cells;
	}

	private String generateComponentItem(String templateName, Node dataField) throws XPathExpressionException {
		String content = generateComponentItem(templateName, dataField == null ? "" : dataField.getTextContent());
		for (String attributeName : attributeRefs(content))
			content = content.replaceAll("\\$\\{attribute:" + attributeName + "\\}",
					Matcher.quoteReplacement(attribute(dataField, attributeName)));
		for (String elementName : elementRefs(content))
			content = content.replaceAll("\\$\\{element:" + elementName + "\\}",
					Matcher.quoteReplacement(element(dataField, elementName)));
		return content;
	}

	private List<String> attributeRefs(String templateContent) {
		List<String> lista = new ArrayList<String>();
		Matcher matcher = Pattern.compile(ATTRIBUTE_REGEX).matcher(templateContent);
		while (matcher.find())
			lista.add(matcher.group(1).trim());
		return lista;
	}

	private List<String> elementRefs(String templateContent) {
		List<String> lista = new ArrayList<String>();
		Matcher matcher = Pattern.compile(ELEMENT_REGEX).matcher(templateContent);
		while (matcher.find())
			lista.add(matcher.group(1).trim());
		return lista;
	}

	private String generateComponentItem(String templateName, String title) {
		return fillMetaData(getTemplate(templateName, null, templatesDir), null, title);
	}

	private String fillMetaData(String text, String id, String title) {
		String content = text;
		if (content.contains(ID_PLACE))
			content = content.replace(ID_PLACE, id == null ? createId(title) : id);
		if (content.contains(NAME_PLACE))
			content = content.replace(NAME_PLACE, standardId(title));
		if (content.contains(TITLE_PLACE))
			content = content.replace(TITLE_PLACE, Matcher.quoteReplacement(title));
		return content;
	}

	private NodeList dataRows(String id, WebComponent component, XPath xpath) throws XPathExpressionException {
		boolean checkTitle = !component.getTitle().isEmpty() && !id.equals(standardId(component.getTitle()));
		NodeList rows = dataRows(getTitle(), id, component, xpath);
		if (rows.getLength() == 0 && checkTitle)
			rows = dataRows(getTitle(), component.getTitle(), component, xpath);
		if (rows.getLength() == 0)
			rows = dataRows(DEFAULT_DATA_CONTEXT, id, component, xpath);
		if (rows.getLength() == 0 && checkTitle)
			rows = dataRows(DEFAULT_DATA_CONTEXT, component.getTitle(), component, xpath);
		return rows;
	}

	private NodeList dataRows(String dataContext, String dataId, WebComponent component, XPath xpath)
			throws XPathExpressionException {
		if (component.getXmlData() == null)
			return (NodeList) xpath.compile("//" + standardId(dataContext) + "/" + dataId + "/*").evaluate(data,
					XPathConstants.NODESET);
		else
			return (NodeList) xpath.compile("*/*").evaluate(component.getXmlData(), XPathConstants.NODESET);
	}

	private String attribute(Node dataItem, String attributeName) {
		Node node = attributeNode(dataItem, attributeName);
		return node == null ? "" : node.getNodeValue();
	}

	private Node attributeNode(Node dataItem, String attributeName) {
		if (!dataItem.hasAttributes())
			return null;
		return dataItem.getAttributes().getNamedItem(attributeName);
	}

	private String element(Node dataItem, String elementName) {
		Node node = elementNode(dataItem, elementName);
		return node == null ? "" : node.getTextContent();
	}

	private Node elementNode(Node dataItem, String elementName) {
		NodeList list = dataItem.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(elementName))
				return node;
		}
		return null;
	}

	private String getDescription(String field) {
		return behavior(field, PROP_DESCRIPTION, "");
	}

	private String getPlaceHolder(String field) {
		return behavior(field, PROP_PLACEHOLDER, "");
	}

	private String getInput(String field) {
		return behavior(field, PROP_INPUT, "text-input");
	}

	private String behavior(String field, String property, String defaultValue) {
		String value = dataBehavior.get(resolveField(field.toLowerCase()) + ":" + property);
		return value == null ? defaultValue : value;
	}

	private String resolveField(String field) {
		if (dataAlias.containsKey(field))
			return resolveField(dataAlias.get(field));
		return field;
	}

	private String pushContext(String id) {
		String newPlace = "${content" + id + "}";
		parentContext.push(newPlace);
		return newPlace;
	}

	protected String createId(String context) {
		String id = standardId(context);
		if (components.contains(id)) {
			int seq = 1;
			while (components.contains(id + "_" + seq))
				seq++;
			id += "_" + seq;
		}
		components.add(id);
		return id;
	}

	protected String createId(WebComponent component) {
		String id = standardId(component.getTitle().isEmpty() ? "_" + component.getType() : component.getTitle());
		if (components.contains(id)) {
			int seq = 1;
			while (components.contains(id + "_" + seq))
				seq++;
			id += "_" + seq;
		}
		components.add(id);
		return id;
	}

	private String quote(String text) {
		return text.replaceAll("\"", "&quot;");
	}

	public void removeAllContentPlaces() {
		setContent(getContent().replaceAll("\\s*\\$\\{content[^\\}]*\\}", ""));
	}

	public void removeAllEmptyCaptions() {
		setContent(getContent().replaceAll("<legend></legend>", "").replaceAll("<caption></caption>", "")
				.replaceAll("<h2[^>]*></h2>", ""));
	}

	public void removeAllEmptyAttributes() {
		setContent(getContent().replaceAll("\\s*[a-z\\-_]*=\"\"", ""));
	}
}
