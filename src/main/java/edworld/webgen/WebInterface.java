// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edworld.util.StreamUtil;

public class WebInterface {
	private static final String PARAMETER_SUFFIX_REGEX = "::.*";
	protected static final String CONTENT_REGEX = "\\$\\{content\\}";
	protected static final String ITEM_TEMPLATE_REGEX = "\\$\\{data:([^\\}]+)\\}";
	protected static final String HEADER_TEMPLATE_REGEX = "\\$\\{header:([^\\}]+)\\}";
	protected static final String ATTRIBUTE_REGEX = "\\$\\{attribute:([^\\}]+)\\}";
	protected static final String LINE_BREAK = System.getProperty("line.separator");
	protected static final String ID_PLACE = "${id}";
	protected static final String CONTENT_PLACE = "${content}";
	protected static final String PROP_DESCRIPTION = "description";
	protected static final String PROP_PLACEHOLDER = "placeholder";
	protected static final String PROP_INPUT = "input";
	protected static final String DEFAULT_DATA_CONTEXT = "default";
	protected String specification;
	protected String defaultLanguage;
	protected File templatesDir;
	protected Document data;
	protected String webGenReportTitle = "WebGen report";
	protected List<WebArtifact> artifacts = new ArrayList<WebArtifact>();
	protected List<WebArtifact> reports = new ArrayList<WebArtifact>();
	protected WebArtifact currentArtifact;
	protected int numberOfDataInputs;
	protected int numberOfDataOutputs;
	protected Stack<String> parentContext = new Stack<String>();
	protected StreamUtil textUtil = new StreamUtil();
	protected List<String> components = new ArrayList<String>();
	protected Map<String, String> dataBehavior;
	protected Map<String, String> dataAlias;
	protected String currentField;

	/**
	 * WebInterface to be expressed into a set of web artifacts according to the specification and the optional data.
	 * 
	 * @param specification
	 *            the specification, expressed as wiki text, for generating the web artifacts
	 * @param dataDictionary
	 *            optional data dictionary for configuring the behavior of data entry and/or presenting
	 * @param defaultLanguage
	 *            the main language in which the web artifacts will be generated
	 * @param templatesDir
	 *            the directory used to override the built-in templates
	 * @param data
	 *            optional (sample) data expressed as XML
	 */
	public WebInterface(String specification, String dataDictionary, String defaultLanguage, File templatesDir, String data) {
		this.specification = specification;
		loadDataBehavior(dataDictionary);
		this.defaultLanguage = defaultLanguage;
		this.templatesDir = templatesDir;
		if (data == null) {
			this.data = null;
			return;
		}
		try {
			this.data = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(data.getBytes()));
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	private void loadDataBehavior(String dataDictionary) {
		dataBehavior = new HashMap<String, String>();
		dataAlias = new HashMap<String, String>();
		if (dataDictionary == null)
			return;
		currentField = null;
		for (String line : getLines(dataDictionary))
			if (!newField(line) && currentField != null)
				updateField(currentField, line);
	}

	private boolean newField(String line) {
		if (line.matches("\\s*\\*[^\\*].*")) {
			String fieldDefinition = line.substring(line.indexOf('*') + 1).trim();
			String[] metadata;
			if (fieldDefinition.contains("=")) {
				metadata = fieldDefinition.split("=");
				currentField = metadata[0].toLowerCase().trim();
				addDataAlias(currentField, metadata[1].toLowerCase().trim());
			} else if (fieldDefinition.contains(":")) {
				metadata = fieldDefinition.split(":", 2);
				currentField = metadata[0].toLowerCase().trim();
				addDataBehavior(currentField, PROP_DESCRIPTION, metadata[1].trim());
			} else
				currentField = fieldDefinition.toLowerCase().trim();
			return true;
		}
		return false;
	}

	private void updateField(String field, String line) {
		Matcher matcher = Pattern.compile("\\s*\\*\\*[^\\*](.*):(.*)").matcher(line);
		if (matcher.find())
			addDataBehavior(field, matcher.group(1).trim(), matcher.group(2).trim());
	}

	private void addDataAlias(String field, String sourceField) {
		if (!resolveField(sourceField).equals(field))
			dataAlias.put(field, sourceField);
	}

	private void addDataBehavior(String field, String property, String value) {
		dataBehavior.put(field + ":" + property, value);
	}

	/**
	 * WebInterface to be expressed into a set of web artifacts according to the specification and the optional data.
	 * 
	 * @param specificationStream
	 *            stream for loading the specification expressed as wiki text, will be closed after this operation
	 * @param dataDicionaryStream
	 *            optional stream for loading the data dictionary expressed as wiki text, will be closed after this operation
	 * @param defaultLanguage
	 *            the main language in which the web artifacts will be generated
	 * @param templatesDir
	 *            the directory for overriding the built-in templates
	 * @param dataStream
	 *            the stream for loading (sample) data expressed as XML, will be closed after this operation
	 */
	public WebInterface(InputStream specificationStream, InputStream dataDicionaryStream, String defaultLanguage, File templatesDir, InputStream dataStream) {
		this(extractText(specificationStream), extractText(dataDicionaryStream), defaultLanguage, templatesDir, extractText(dataStream));
	}

	private static String extractText(InputStream stream) {
		return new StreamUtil().extractText(stream);
	}

	public void generateArtifacts() {
		artifacts.clear();
		currentArtifact = null;
		numberOfDataInputs = 0;
		numberOfDataOutputs = 0;
		components.clear();
		parentContext.clear();
		for (String line : getLines(specification))
			if (!newArtifact(line) && currentArtifact != null)
				updateArtifact(currentArtifact, line);
		autoMenu();
		removeAllContentPlaces();
		removeAllEmptyCaptions();
		removeAllEmptyAttributes();
		reports = null;
	}

	private void generateReports() {
		String data = "<" + standardId(getWebGenReportTitle()) + ">" + LINE_BREAK;
		data += buildArtifactTableData() + LINE_BREAK;
		data += "</" + standardId(getWebGenReportTitle()) + ">";
		WebInterface webReports = new WebInterface(getTemplate("webgen-reporting-specification.wiki"), null, defaultLanguage, templatesDir, data);
		webReports.generateArtifacts();
		reports = webReports.getArtifacts();
	}

	private String buildArtifactTableData() {
		String xml = "<_table>" + LINE_BREAK;
		for (WebArtifact artifact : artifacts) {
			xml += "<artifact>" + LINE_BREAK;
			xml += "<title>" + encodeCharData(addLink(artifact.getTitle(), artifact.getFileName())) + "</title>" + LINE_BREAK;
			xml += "<data_inputs>" + artifact.getDataInputs() + "</data_inputs>" + LINE_BREAK;
			xml += "<data_outputs>" + artifact.getDataOutputs() + "</data_outputs>" + LINE_BREAK;
			xml += "</artifact>" + LINE_BREAK;
		}
		xml += "</_table>";
		return xml;
	}

	private String addLink(String text, String link) {
		return "<a href=\"" + link + "\">" + text + "</a>";
	}

	private boolean newArtifact(String line) {
		if (line.matches("\\s*==[^=].*")) {
			String title = line.replaceAll("==", "").trim();
			currentArtifact = new WebArtifact(title, generateWebPage(title, defaultLanguage), standardId(title) + ".html");
			artifacts.add(currentArtifact);
			numberOfDataInputs = 0;
			numberOfDataOutputs = 0;
			components.clear();
			parentContext.clear();
			parentContext.push(CONTENT_PLACE);
			return true;
		}
		return false;
	}

	private void updateArtifact(WebArtifact artifact, String line) {
		updateLevel(line);
		String contentPlace = parentContext.peek();
		String content = artifact.getContent();
		content = apply(component(line), content, contentPlace);
		artifact.setContent(content);
		artifact.setDataInputs(numberOfDataInputs);
		artifact.setDataOutputs(numberOfDataOutputs);
	}

	private String generateWebPage(String title, String lang) {
		return getTemplate("web-page.html").replaceAll("\\$\\{lang\\}", lang).replaceAll("\\$\\{title\\}", title);
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
			numberOfDataInputs++;
		return parent.replace(place, component + place);
	}

	private String component(String line) {
		WebComponent component = new WebComponent(line);
		String id = createId(component);
		String contentPlace = pushContext(id);
		String content = getTemplate(standardId(component.getType()) + ".html").replaceAll("\\$\\{id\\}", id).replaceAll("\\$\\{title\\}",
				Matcher.quoteReplacement(component.getTitle()));
		content = resolveHeader(id, component, content);
		content = resolveData(id, component, content);
		if (content.toLowerCase().contains("</form>"))
			return content.replaceAll(CONTENT_REGEX, Matcher.quoteReplacement(generateInputFields(component.getParameters()) + contentPlace)) + LINE_BREAK;
		else
			return content.replaceAll(CONTENT_REGEX, Matcher.quoteReplacement(generateGenericContent(component.getParameters()) + contentPlace)) + LINE_BREAK;
	}

	private String generateGenericContent(String[] parameters) {
		String result = "";
		for (String parameter : parameters)
			result += parameter + LINE_BREAK;
		return result;
	}

	private String data(String dataId) {
		numberOfDataOutputs++;
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			return xpath.compile("//" + standardId(currentArtifact.getTitle()) + "/" + dataId + "/text()").evaluate(data);
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
	}

	private String generateInputFields(String[] fields) {
		String result = "";
		for (String field : fields)
			result += generateTextInput(field, getDescription(field), getPlaceHolder(field), "") + LINE_BREAK;
		return result;
	}

	private String generateTextInput(String field, String description, String placeHolder, String value) {
		numberOfDataInputs++;
		String title = field;
		String id = createId(title);
		String templateName = getInput(field);
		return resolveData(id, new WebComponent("{" + templateName + " " + title + "}"),
				getTemplate(templateName + ".html").replaceAll("\\$\\{id\\}", quote(id)).replaceAll("\\$\\{title\\}", title).replaceAll("\\$\\{description\\}", quote(description))
						.replaceAll("\\$\\{placeholder\\}", quote(placeHolder)).replaceAll("\\$\\{value\\}", quote(value)));
	}

	private String resolveData(String id, WebComponent component, String content) {
		if (content.contains("${data}"))
			content = content.replaceAll("\\$\\{data\\}", Matcher.quoteReplacement(data(id)));
		Matcher matcher = Pattern.compile(ITEM_TEMPLATE_REGEX).matcher(content);
		while (matcher.find()) {
			String[] templates = matcher.group(1).split("@", 2);
			String rowTemplate = templates.length < 2 ? CONTENT_PLACE : getTemplate(templates[1] + ".html");
			content = content.replaceAll("\\$\\{data:" + matcher.group(1) + "\\}", Matcher.quoteReplacement(buildComponentData(templates[0], id, component, rowTemplate)));
		}
		return content;
	}

	private String resolveHeader(String id, WebComponent component, String content) {
		Matcher matcher = Pattern.compile(HEADER_TEMPLATE_REGEX).matcher(content);
		while (matcher.find()) {
			String[] templates = matcher.group(1).split("@", 2);
			String rowTemplate = templates.length < 2 ? CONTENT_PLACE : getTemplate(templates[1] + ".html");
			content = content.replaceAll("\\$\\{header:" + matcher.group(1) + "\\}", Matcher.quoteReplacement(buildComponentHeader(templates[0], component, rowTemplate)));
		}
		return content;
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
		numberOfDataOutputs += Math.max(1, component.getParameters().length);
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

	private String generateComponentItem(String templateName, Node dataField) throws XPathExpressionException {
		String content = generateComponentItem(templateName, dataField == null ? "" : dataField.getTextContent());
		Matcher matcher = Pattern.compile(ATTRIBUTE_REGEX).matcher(content);
		while (matcher.find())
			content = content.replaceAll("\\$\\{attribute:" + matcher.group(1).trim() + "\\}", Matcher.quoteReplacement(attribute(dataField, matcher.group(1).trim())));
		return content;
	}

	private String generateComponentItem(String templateName, String title) {
		String content = getTemplate(templateName + ".html");
		if (content.contains(ID_PLACE))
			content = content.replaceAll("\\$\\{id\\}", createId(title));
		return content.replaceAll("\\$\\{title\\}", Matcher.quoteReplacement(title));
	}

	private Node[] getDataFields(String[] fields, XPath xpath, Node dataItem) throws XPathExpressionException {
		if (fields.length == 0)
			return new Node[] { dataItem.getFirstChild() };
		Node[] cells = new Node[fields.length];
		for (int i = 0; i < cells.length; i++)
			cells[i] = (Node) xpath.compile(standardId(parameterName(fields[i]))).evaluate(dataItem, XPathConstants.NODE);
		return cells;
	}

	private String attribute(Node dataItem, String attributeName) {
		if ("#cdata-section".equals(dataItem.getNodeName()) || "#text".equals(dataItem.getNodeName()))
			dataItem = dataItem.getParentNode();
		if (!dataItem.hasAttributes())
			return "";
		Node node = dataItem.getAttributes().getNamedItem(attributeName);
		if (node == null)
			return "";
		return node.getNodeValue();
	}

	private NodeList dataRows(String id, WebComponent component, XPath xpath) throws XPathExpressionException {
		boolean checkTitle = !component.getTitle().isEmpty() && !id.equals(standardId(component.getTitle()));
		NodeList rows = dataRows(currentArtifact.getTitle(), id, xpath);
		if (rows.getLength() == 0 && checkTitle)
			rows = dataRows(currentArtifact.getTitle(), component.getTitle(), xpath);
		if (rows.getLength() == 0)
			rows = dataRows(DEFAULT_DATA_CONTEXT, id, xpath);
		if (rows.getLength() == 0 && checkTitle)
			rows = dataRows(DEFAULT_DATA_CONTEXT, component.getTitle(), xpath);
		return rows;
	}

	private NodeList dataRows(String dataContext, String dataId, XPath xpath) throws XPathExpressionException {
		return (NodeList) xpath.compile("//" + standardId(dataContext) + "/" + dataId + "/*").evaluate(data, XPathConstants.NODESET);
	}

	private String quote(String text) {
		return text.replaceAll("\"", "&quot;");
	}

	public String getTemplate(String fileName) {
		File templateFile = new File(templatesDir, fileName);
		if (templateFile.exists())
			try {
				return textUtil.extractText(new FileInputStream(templateFile));
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		InputStream templateStream = WebInterface.class.getResourceAsStream("/templates/" + fileName);
		if (templateStream == null)
			throw new IllegalArgumentException("Template resource not found: " + fileName);
		return textUtil.extractText(templateStream);
	}

	private void autoMenu() {
		String autoMenu = "";
		String separator = "";
		for (WebArtifact artifact : artifacts) {
			autoMenu += separator
					+ getTemplate("menu-item.html").replaceAll("\\$\\{url\\}", Matcher.quoteReplacement(artifact.getFileName())).replaceAll("\\$\\{title\\}",
							Matcher.quoteReplacement(artifact.getTitle()));
			separator = LINE_BREAK;
		}
		for (WebArtifact artifact : artifacts)
			artifact.setContent(artifact.getContent().replaceAll("\\$\\{automenu:menu-item\\}", Matcher.quoteReplacement(autoMenu)));
	}

	private void removeAllContentPlaces() {
		for (WebArtifact artifact : artifacts)
			artifact.setContent(artifact.getContent().replaceAll("\\s*\\$\\{content[^\\}]*\\}", ""));
	}

	private void removeAllEmptyCaptions() {
		for (WebArtifact artifact : artifacts)
			artifact.setContent(artifact.getContent().replaceAll("<legend></legend>", "").replaceAll("<caption></caption>", "").replaceAll("<h2[^>]*></h2>", ""));
	}

	private void removeAllEmptyAttributes() {
		for (WebArtifact artifact : artifacts)
			artifact.setContent(artifact.getContent().replaceAll("\\s*[a-z\\-_]*=\"\"", ""));
	}

	public String pushContext(String id) {
		String newPlace = "${content" + id + "}";
		parentContext.push(newPlace);
		return newPlace;
	}

	public String createId(String context) {
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

	public String createId(WebComponent component) {
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

	public void saveArtifactsToDir(File dir) throws IOException {
		dir.mkdirs();
		for (WebArtifact artifact : artifacts)
			save(artifact, dir);
	}

	public void saveReportsToDir(File dir) throws IOException {
		dir.mkdirs();
		for (WebArtifact report : getReports())
			save(report, dir);
	}

	private void save(WebArtifact artifact, File dir) throws IOException {
		PrintWriter out = new PrintWriter(new File(dir, artifact.getFileName()));
		try {
			out.write(artifact.getContent());
		} finally {
			out.close();
		}
	}

	public String standardId(String context) {
		return textUtil.removeDiacritics(context.toLowerCase()).replaceAll(" ", "_").replaceAll("[.,:;'\"?!]", "");
	}

	private String encodeCharData(String text) {
		return "<![CDATA[" + text + "]]>";
	}

	public List<WebArtifact> getArtifacts() {
		return artifacts;
	}

	public List<WebArtifact> getReports() {
		if (reports == null)
			generateReports();
		return reports;
	}

	public String getWebGenReportTitle() {
		return webGenReportTitle;
	}

	public void setWebGenReportTitle(String webGenReportTitle) {
		this.webGenReportTitle = webGenReportTitle;
	}

	private String[] getLines(String text) {
		return text.split("\r\n?|\n");
	}
}
