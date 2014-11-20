package util;

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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WebInterface {
	private static final String ID_PLACE = "${id}";
	private static final String CONTENT_PLACE = "${content}";
	private static final String LINE_BREAK = System.getProperty("line.separator");
	private String specification;
	private String defaultLanguage;
	private File templatesDir;
	private Document data;
	private Map<String, WebArtifact> artifacts = new HashMap<String, WebArtifact>();
	private Map<String, WebArtifact> reports = new HashMap<String, WebArtifact>();
	private WebArtifact currentArtifact;
	private int numberOfDataInputs;
	private int numberOfDataOutputs;
	private Stack<String> parentContext = new Stack<String>();
	private StreamUtil textUtil = new StreamUtil();
	private List<String> components = new ArrayList<String>();

	/**
	 * WebInterface to be expressed into a set of web artifacts according to the
	 * specification and the optional data.
	 * 
	 * @param specification
	 *            the specification, expressed as wiki text, used to generate
	 *            the web artifacts
	 * @param defaultLanguage
	 *            the main language used to express the web artifacts
	 * @param templatesDir
	 *            the directory used to override the built-in templates
	 * @param data
	 *            optional data expressed as XML
	 */
	public WebInterface(String specification, String defaultLanguage, File templatesDir, String data) {
		this.specification = specification;
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

	/**
	 * WebInterface to be expressed into a set of web artifacts according to the
	 * specification and the optional data.
	 * 
	 * @param specificationStream
	 *            the stream used to load specification, expressed as wiki text,
	 *            will be closed after this operation
	 * @param defaultLanguage
	 *            the main language used to express the web artifacts
	 * @param templatesDir
	 *            the directory used to override the built-in templates
	 * @param dataStream
	 *            the stream used to load data, expressed as XML, will be closed
	 *            after this operation
	 */
	public WebInterface(InputStream specificationStream, String defaultLanguage, File templatesDir, InputStream dataStream) {
		this(extractText(specificationStream), defaultLanguage, templatesDir, extractText(dataStream));
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
		for (String line : specification.split("\r\n?|\n")) {
			if (!newArtifact(line) && currentArtifact != null) {
				updateArtifact(currentArtifact, line);
			}
		}
		removeAllContentPlaces();
		removeAllEmptyCaptions();
		reports = null;
	}

	private void generateReports() {
		String data = "<webgen_report>" + LINE_BREAK;
		data += buildArtifactTableData() + LINE_BREAK;
		data += "</webgen_report>";
		WebInterface webReports = new WebInterface(getTemplate("webgen-reporting-specification.wiki"), defaultLanguage, templatesDir, data);
		webReports.generateArtifacts();
		reports = webReports.getArtifacts();
	}

	private String buildArtifactTableData() {
		String xml = "<_table>" + LINE_BREAK;
		for (WebArtifact artifact : artifacts.values()) {
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
			artifacts.put(title, currentArtifact);
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
		if (component.toLowerCase().contains("</ul>"))
			numberOfDataOutputs++;
		return parent.replace(place, component + place);
	}

	private String component(String line) {
		if (WebComponent.matches(line)) {
			WebComponent component = new WebComponent(line);
			String id = createId(component);
			String contentPlace = pushContext(id);
			String content = getTemplate(standardId(component.getType()) + ".html").replaceAll("\\$\\{id\\}", id).replaceAll("\\$\\{title\\}",
					Matcher.quoteReplacement(component.getTitle()));
			if (content.toLowerCase().contains("</table>"))
				return content.replaceAll("\\$\\{content_header\\}", Matcher.quoteReplacement(generateTableHeader(component.getParameters()))).replaceAll("\\$\\{content\\}",
						Matcher.quoteReplacement(buildTableData(id, component.getParameters())));
			else if (content.toLowerCase().contains("</ul>"))
				return content.replaceAll("\\$\\{content\\}", Matcher.quoteReplacement(buildListData(id)));
			else
				return content.replaceAll("\\$\\{content\\}", Matcher.quoteReplacement(generateInputFields(component.getParameters()) + contentPlace)) + LINE_BREAK;
		}
		return "";
	}

	private String buildTableData(String id, String[] fields) {
		if (data == null)
			return "";
		String content = "";
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			NodeList rows = (NodeList) xpath.compile("//" + standardId(currentArtifact.getTitle()) + "/" + id + "/*").evaluate(data, XPathConstants.NODESET);
			for (int i = 0; i < rows.getLength(); i++) {
				String[] cells = new String[fields.length];
				int j = 0;
				for (String field : fields) {
					cells[j] = xpath.compile(standardId(field) + "/text()").evaluate(rows.item(i));
					j++;
				}
				content += generateTableBodyRow(cells) + LINE_BREAK;
			}
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
		return content;
	}

	private String buildListData(String id) {
		if (data == null)
			return "";
		String content = "";
		XPath xpath = XPathFactory.newInstance().newXPath();
		try {
			NodeList rows = (NodeList) xpath.compile("//" + standardId(currentArtifact.getTitle()) + "/" + id + "/*").evaluate(data, XPathConstants.NODESET);
			for (int i = 0; i < rows.getLength(); i++) {
				Node item = rows.item(i).getFirstChild();
				content += generateListItem(item.getTextContent(), attributeClass(item)) + LINE_BREAK;
			}
		} catch (XPathExpressionException e) {
			throw new IllegalArgumentException(e);
		}
		return content;
	}

	private String attributeClass(Node item) {
		if ("#cdata-section".equals(item.getNodeName())) {
			item = item.getParentNode();
		}
		if (!item.hasAttributes())
			return "";
		Node node = item.getAttributes().getNamedItem("class");
		if (node == null)
			return "";
		return " " + node.getNodeValue();
	}

	private String generateInputFields(String[] fields) {
		String result = "";
		for (String field : fields) {
			result += generateTextInput(field, "") + LINE_BREAK;
		}
		return result;
	}

	private String generateTableHeader(String[] fields) {
		String result = "<tr>";
		for (String field : fields) {
			result += generateTableHeaderCell(field);
		}
		return result + "</tr>";
	}

	private String generateTableBodyRow(String[] cells) {
		String result = "<tr>";
		for (String cell : cells) {
			result += generateTableBodyCell(cell);
		}
		return result + "</tr>";
	}

	private String generateTextInput(String title, String value) {
		numberOfDataInputs++;
		String id = createId(title);
		return getTemplate("text-input.html").replaceAll("\\$\\{id\\}", id).replaceAll("\\$\\{title\\}", title).replaceAll("\\$\\{value\\}", value);
	}

	private String generateTableHeaderCell(String title) {
		numberOfDataOutputs++;
		String template = getTemplate("table-header-cell.html");
		if (template.contains(ID_PLACE))
			template = template.replaceAll("\\$\\{id\\}", createId(title));
		return template.replaceAll("\\$\\{title\\}", title);
	}

	private String generateTableBodyCell(String title) {
		String template = getTemplate("table-body-cell.html");
		if (template.contains(ID_PLACE))
			template = template.replaceAll("\\$\\{id\\}", createId(title));
		return template.replaceAll("\\$\\{title\\}", title);
	}

	private String generateListItem(String item, String attributeClass) {
		String template = getTemplate("list-item.html");
		if (template.contains(ID_PLACE))
			template = template.replaceAll("\\$\\{id\\}", createId(item));
		return template.replaceAll("\\$\\{content\\}", item).replaceAll("\\$\\{class\\}", attributeClass);
	}

	public String getTemplate(String fileName) {
		File templateFile = new File(templatesDir, fileName);
		if (templateFile.exists())
			try {
				return textUtil.extractText(new FileInputStream(templateFile));
			} catch (FileNotFoundException e) {
				throw new IllegalArgumentException(e);
			}
		return textUtil.extractText(getClass().getResourceAsStream("/templates/" + fileName));
	}

	private void removeAllContentPlaces() {
		for (WebArtifact artifact : artifacts.values()) {
			artifact.setContent(artifact.getContent().replaceAll("\\s*\\$\\{content[^\\}]*\\}", ""));
		}
	}

	private void removeAllEmptyCaptions() {
		for (WebArtifact artifact : artifacts.values()) {
			artifact.setContent(artifact.getContent().replaceAll("<legend></legend>", "").replaceAll("<caption></caption>", "").replaceAll("<h2[^>]*></h2>", ""));
		}
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
			while (components.contains(id + "_" + seq)) {
				seq++;
			}
			id += "_" + seq;
		}
		components.add(id);
		return id;
	}

	public String createId(WebComponent component) {
		String id = standardId(component.getTitle().isEmpty() ? "_" + component.getType() : component.getTitle());
		if (components.contains(id)) {
			int seq = 1;
			while (components.contains(id + "_" + seq)) {
				seq++;
			}
			id += "_" + seq;
		}
		components.add(id);
		return id;
	}

	public void saveArtifactsToDir(File dir) throws IOException {
		dir.mkdirs();
		for (WebArtifact artifact : artifacts.values()) {
			save(artifact, dir);
		}
	}

	public void saveReportsToDir(File dir) throws IOException {
		dir.mkdirs();
		for (WebArtifact report : getReports().values()) {
			save(report, dir);
		}
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
		return textUtil.removeDiacritics(context.toLowerCase()).replaceAll(" ", "_");
	}

	private String encodeCharData(String text) {
		return "<![CDATA[" + text + "]]>";
	}

	public Map<String, WebArtifact> getArtifacts() {
		return artifacts;
	}

	public Map<String, WebArtifact> getReports() {
		if (reports == null) {
			generateReports();
		}
		return reports;
	}
}
