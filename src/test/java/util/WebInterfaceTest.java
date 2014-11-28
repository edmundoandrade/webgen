package util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class WebInterfaceTest {
	private WebInterface webInterface;

	@Before
	public void setUp() throws IOException {
		webInterface = new WebInterface(getSpecification("/web-interface-specification.wiki"), getSpecification("/data-dictionary.wiki"), "en", new File("target/web-templates"),
				getSpecification("/sample-data.xml"));
		webInterface.generateArtifacts();
	}

	@Test
	public void ignoreAnyInformationBeforeFirstMark() {
		for (WebArtifact artifact : webInterface.getArtifacts().values()) {
			assertThat(artifact.getContent(), not(containsString("Any information")));
		}
	}

	@Test
	public void generateWebPages() {
		assertEquals(2, webInterface.getArtifacts().size());
		String content1 = webInterface.getArtifacts().get("Main page").getContent();
		String content2 = webInterface.getArtifacts().get("New official document").getContent();
		assertThat(content1, containsString("<html lang=\"en\">"));
		assertThat(content1, containsString("<title>Main page</title>"));
		assertThat(content2, containsString("<html lang=\"en\">"));
		assertThat(content2, containsString("<title>New official document</title>"));
	}

	@Test
	public void generateSections() {
		String content = webInterface.getArtifacts().get("Main page").getContent();
		assertThat(content, containsString("<section id=\"section_a\" aria-labelledby=\"section_a_heading\"><h2 id=\"section_a_heading\">Section A</h2>"));
		assertThat(content, containsString("<section id=\"secao_x\" aria-labelledby=\"secao_x_heading\"><h2 id=\"secao_x_heading\">Seção X</h2>"));
	}

	@Test
	public void generateFilters() {
		String content = webInterface.getArtifacts().get("Main page").getContent();
		assertThat(content, containsString("<form class=\"filter\" id=\"f\"><fieldset>"));
		assertThat(content, not(containsString("<legend></legend>")));
	}

	@Test
	public void generateTables() {
		String content = webInterface.getArtifacts().get("Main page").getContent();
		assertThat(content, containsString("<table id=\"_table\">"));
		assertThat(content, not(containsString("<caption></caption>")));
		assertThat(content, containsString("<thead><tr><th>THeader1</th><th>THeader2</th><th>THeader3</th><th>THeader4</th></tr></thead>"));
		assertThat(content, containsString("<tr><td>NONONONX</td><td>NONONONY</td><td>NONONONZ</td><td><a href=\"#section_a\">Click</a></td></tr>"));
		assertThat(content, containsString("<tr><td>HOHOHOHA</td><td>HOHOHOHB</td><td>HOHOHOHC</td><td><a href=\"#secao_x\">Click</a></td></tr>"));
		assertThat(content, containsString("<thead><tr><th>TCab1</th><th>TCab2</th><th>TCab3</th></tr></thead>"));
		assertThat(content, containsString("<tr><td><a href=\"#\">Item 1</a></td><td>NONONONY</td><td>NONONONZ</td></tr>"));
	}

	@Test
	public void generateTextInputs() {
		String content = webInterface.getArtifacts().get("Main page").getContent();
		assertThat(content, containsString("<label>FInput1<input type=\"text\" id=\"finput1\"></label>"));
		assertThat(content, containsString("<label>FInput2<select id=\"finput2\"></select></label>"));
		assertThat(content, containsString("<label title=\"document &quot;classifier&quot; according to the international standards\">Document type<select id=\"document_type\">"
				+ documentTypeOptions() + "</select></label>"));
		assertThat(content, containsString("<label>FEntrada1<input type=\"text\" id=\"fentrada1\"></label>"));
		assertThat(content, containsString("<label>Filtro entrada dois<input type=\"text\" id=\"filtro_entrada_dois\"></label>"));
		content = webInterface.getArtifacts().get("New official document").getContent();
		assertThat(content, containsString("<label>Name<input type=\"text\" id=\"name\" placeholder=\"type the document's name or title\"></label>"));
		assertThat(content, containsString("<label title=\"document &quot;classifier&quot; according to the international standards\">Document type<select id=\"document_type\">"
				+ documentTypeOptions() + "</select></label>"));
		assertThat(content, containsString("<label>Owner<input type=\"text\" id=\"owner\"></label>"));
	}

	private String documentTypeOptions() {
		String lineSep = System.getProperty("line.separator");
		return "<option>(Undefined document)</option>" + lineSep + "<option value=\"int\">Internal document</option>" + lineSep + "<option value=\"fed\">Federal document</option>"
				+ lineSep;
	}

	@Test
	public void generateActions() {
		String content = webInterface.getArtifacts().get("Main page").getContent();
		assertThat(content, containsString("<a href=\"one.html\"><button type=\"button\">one</button></a>"));
		assertThat(content, containsString("<a href=\"acao_alfa.html\"><button type=\"button\">Ação alfa</button></a>"));
	}

	@Test
	public void generateLists() {
		String content = webInterface.getArtifacts().get("New official document").getContent();
		assertThat(content, containsString("<ul id=\"my_list\" aria-labelledby=\"my_list_heading\"><h2 id=\"my_list_heading\">My list</h2>"));
		assertThat(content, containsString("<li class=\" title\"><strong>Sample data</strong></li>"));
		assertThat(content, containsString("<li>See the file <em>sample-data.xml</em> to configure sample data presented here!</li>"));
		assertThat(content, containsString("<ul id=\"_list\" aria-labelledby=\"_list_heading\">"));
		assertThat(content, not(containsString("></h2>")));
	}

	@Test
	public void generateReports() {
		String content = webInterface.getReports().get("WebGen report").getContent();
		assertThat(content, containsString("<html lang=\"en\">"));
		assertThat(content, containsString("<title>WebGen report</title>"));
		assertThat(content, containsString("<thead><tr><th>Title</th><th>Data inputs</th><th>Data outputs</th></tr></thead>"));
		assertThat(content, containsString("<tr><td><a href=\"main_page.html\">Main page</a></td><td>9</td><td>7</td></tr>"));
		assertThat(content, containsString("<tr><td><a href=\"new_official_document.html\">New official document</a></td><td>4</td><td>2</td></tr>"));
	}

	@Test
	public void saveArtifactsToDir() throws IOException {
		File dir = new File("target/web-test");
		File artifactFile1 = new File(dir, "main_page.html");
		File artifactFile2 = new File(dir, "new_official_document.html");
		artifactFile1.delete();
		artifactFile2.delete();
		webInterface.saveArtifactsToDir(dir);
		assertThat(artifactFile1.exists(), is(true));
		assertThat(artifactFile2.exists(), is(true));
	}

	@Test
	public void saveReportsToDir() throws IOException {
		File dir = new File("target/web-test");
		File reportFile = new File(dir, "webgen_report.html");
		reportFile.delete();
		webInterface.saveReportsToDir(dir);
		assertThat(reportFile.exists(), is(true));
	}

	private String getSpecification(String resourceName) {
		return new StreamUtil().extractText(getClass().getResourceAsStream(resourceName));
	}
}
