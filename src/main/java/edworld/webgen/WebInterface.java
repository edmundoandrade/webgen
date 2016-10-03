// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;

import edworld.util.TextUtil;

public class WebInterface {
	protected static final String PROP_DESCRIPTION = "description";
	protected static final String LINE_BREAK = System.getProperty("line.separator");
	protected static TextUtil textUtil = new TextUtil();

	protected String specification;
	protected String defaultLanguage;
	protected WebTemplateFinder templateFinder;
	protected Document data;
	protected String webGenReportTitle = "WebGen report";
	protected List<WebArtifact> artifacts = new ArrayList<WebArtifact>();
	protected List<WebArtifact> reports = new ArrayList<WebArtifact>();
	protected WebArtifact currentArtifact;
	protected Map<String, String> dataBehavior;
	protected Map<String, String> dataAlias;
	protected String currentField;
	protected String charSet = "UTF-8";

	/**
	 * WebInterface to be expressed into a set of web artifacts according to the
	 * specification and the optional data.
	 * 
	 * @param specification
	 *            the specification, expressed as wiki text, for generating the
	 *            web artifacts
	 * @param dataDictionary
	 *            optional data dictionary for configuring the behavior of data
	 *            entry and/or presenting
	 * @param defaultLanguage
	 *            the main language in which the web artifacts will be generated
	 * @param templateFinder
	 *            a custom finder for locating/overriding built-in templates
	 * @param data
	 *            optional (sample) data expressed as XML
	 */
	public WebInterface(String specification, String dataDictionary, String defaultLanguage,
			WebTemplateFinder templateFinder, String data) {
		this.specification = specification;
		loadDataBehavior(dataDictionary);
		this.defaultLanguage = defaultLanguage;
		this.templateFinder = templateFinder;
		if (data == null) {
			this.data = null;
			return;
		}
		try {
			this.data = DocumentBuilderFactory.newInstance().newDocumentBuilder()
					.parse(new ByteArrayInputStream(StringEscapeUtils.unescapeHtml4(data).getBytes(charSet)));
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
		Matcher matcher = Pattern.compile("\\s*\\*\\*[^\\*]([^:]*):(.*)").matcher(line);
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
	 * WebInterface to be expressed into a set of web artifacts according to the
	 * specification and the optional data.
	 * 
	 * @param specificationStream
	 *            stream for loading the specification expressed as wiki text,
	 *            will be closed after this operation
	 * @param dataDicionaryStream
	 *            optional stream for loading the data dictionary expressed as
	 *            wiki text, will be closed after this operation
	 * @param defaultLanguage
	 *            the main language in which the web artifacts will be generated
	 * @param templateFinder
	 *            a custom finder for locating/overriding built-in templates
	 * @param dataStream
	 *            the stream for loading (sample) data expressed as XML, will be
	 *            closed after this operation
	 */
	public WebInterface(InputStream specificationStream, InputStream dataDicionaryStream, String defaultLanguage,
			WebTemplateFinder templateFinder, InputStream dataStream) {
		this(extractText(specificationStream), extractText(dataDicionaryStream), defaultLanguage, templateFinder,
				extractText(dataStream));
	}

	private static String extractText(InputStream stream) {
		return new TextUtil().extractText(stream);
	}

	public void generateArtifacts() {
		artifacts.clear();
		currentArtifact = null;
		for (String line : getLines(specification))
			if (!newArtifact(line) && currentArtifact != null)
				currentArtifact.updateArtifact(line);
		autoMenu();
		for (WebArtifact artifact : artifacts) {
			artifact.consolidateHeadElements();
			artifact.removeAllContentPlaces();
			artifact.removeAllEmptyCaptions();
			artifact.removeAllEmptyAttributes();
		}
		reports = null;
	}

	protected void generateReports() {
		String data = "<" + textUtil.standardId(getWebGenReportTitle()) + ">" + LINE_BREAK;
		data += buildArtifactTableData() + LINE_BREAK;
		data += "</" + textUtil.standardId(getWebGenReportTitle()) + ">";
		WebInterface webReports = new WebInterface(
				templateFinder.getTemplate("webgen-reporting-specification", null, ".wiki"), null, defaultLanguage,
				templateFinder, data);
		webReports.generateArtifacts();
		reports = webReports.getArtifacts();
	}

	private String buildArtifactTableData() {
		String xml = "<_table>" + LINE_BREAK;
		for (WebArtifact artifact : artifacts) {
			xml += "<artifact>" + LINE_BREAK;
			xml += "<title>" + encodeCharData(addLink(artifact.getTitle(), artifact.getFileName())) + "</title>"
					+ LINE_BREAK;
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

	protected boolean newArtifact(String line) {
		if (line.matches("\\s*==[^=].*")) {
			String title = line.replaceAll("==", "").trim();
			currentArtifact = new WebArtifact(title, generateWebPage(title, defaultLanguage),
					textUtil.standardId(title) + ".html", dataBehavior, dataAlias, templateFinder, data);
			artifacts.add(currentArtifact);
			return true;
		}
		return false;
	}

	private String generateWebPage(String title, String lang) {
		return templateFinder.getTemplate("web-page", null).replaceAll("\\$\\{lang\\}", lang)
				.replaceAll("\\$\\{title\\}", title);
	}

	private void autoMenu() {
		String autoMenu = "";
		String separator = "";
		for (WebArtifact artifact : artifacts) {
			autoMenu += separator + templateFinder.getTemplate("menu-item", null)
					.replaceAll("\\$\\{url\\}", Matcher.quoteReplacement(artifact.getFileName()))
					.replaceAll("\\$\\{title\\}", Matcher.quoteReplacement(artifact.getTitle()));
			separator = LINE_BREAK;
		}
		for (WebArtifact artifact : artifacts)
			artifact.setContent(artifact.getContent().replaceAll("\\$\\{automenu:menu-item\\}",
					Matcher.quoteReplacement(setActiveMenu(autoMenu, artifact))));
	}

	private String setActiveMenu(String content, WebArtifact artifact) {
		return content.replace("<li><a href=\"" + artifact.getFileName() + "\">",
				"<li class=\"active\"><a href=\"" + artifact.getFileName() + "\">");
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
		PrintWriter out = new PrintWriter(new File(dir, artifact.getFileName()), charSet);
		try {
			out.write(artifact.getContent());
		} finally {
			out.close();
		}
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
