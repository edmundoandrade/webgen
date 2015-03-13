// This open source code is distributed without warranties according to the license published at http://www.apache.org/licenses/LICENSE-2.0
package edworld.webgen;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import edworld.util.StreamUtil;

public class WebInterfaceTest {
	private static final int MAIN_PAGE = 0;
	private static final int NEW_OFFICIAL_DOCUMENT = 1;
	private WebInterface webInterface;

	@Before
	public void setUp() throws IOException {
		webInterface = new WebInterface(
				getSpecification("/web-interface-specification.wiki"),
				getSpecification("/data-dictionary.wiki"), "en", new File(
						"target/web-templates"),
				getSpecification("/sample-data.xml"));
		webInterface.generateArtifacts();
	}

	@Test
	public void ignoreAnyInformationBeforeFirstMark() {
		for (WebArtifact artifact : webInterface.getArtifacts())
			assertThat(artifact.getContent(),
					not(containsString("Any information")));
	}

	@Test
	public void generateWebPages() {
		assertEquals(2, webInterface.getArtifacts().size());
		String content1 = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		String content2 = webInterface.getArtifacts()
				.get(NEW_OFFICIAL_DOCUMENT).getContent();
		assertThat(content1, containsString("<html lang=\"en\">"));
		assertThat(content1, containsString("<title>Main page</title>"));
		assertThat(content2, containsString("<html lang=\"en\">"));
		assertThat(content2,
				containsString("<title>New official document</title>"));
	}

	@Test
	public void generateFreeContent() {
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(content, containsString("<hr>free HTML content<hr>"));
		assertThat(content, not(containsString("* <hr>free HTML content<hr>")));
	}

	@Test
	public void generateSections() {
		String content1 = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(
				content1,
				containsString("<section id=\"section_a\" aria-labelledby=\"section_a_heading\"><h2 id=\"section_a_heading\">Section A</h2>"));
		assertThat(
				content1,
				containsString("<section id=\"secao_x\" aria-labelledby=\"secao_x_heading\"><h2 id=\"secao_x_heading\">Seção X</h2>"));
		String content2 = webInterface.getArtifacts()
				.get(NEW_OFFICIAL_DOCUMENT).getContent();
		assertThat(
				content2,
				containsString("<section id=\"dashboard\" aria-labelledby=\"dashboard_heading\"><h2 id=\"dashboard_heading\">Dashboard</h2>"));
	}

	@Test
	public void generateFilters() {
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(content,
				containsString("<form class=\"filter\" id=\"f\"><fieldset>"));
		assertThat(content, not(containsString("<legend></legend>")));
	}

	@Test
	public void generateTables() {
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(content, containsString("<table id=\"_table\">"));
		assertThat(content, not(containsString("<caption></caption>")));
		assertThat(
				content,
				containsString("<thead><tr><th>THeader1</th><th>THeader2</th><th>THeader3</th><th>THeader4</th></tr></thead>"));
		assertThat(
				content,
				containsString("<tr><td>NONONONX</td><td>NONONONY</td><td>NONONONZ</td><td><a href=\"#section_a\">Click</a></td></tr>"));
		assertThat(
				content,
				containsString("<tr><td>HOHOHOHA</td><td class=\"row\" data-title=\"t2\">HOHOHOHB</td><td>HOHOHOHC</td><td><a href=\"#secao_x\">Click</a></td></tr>"));
		assertThat(
				content,
				containsString("<thead><tr><th>TCab1</th><th>TCab2</th><th>TCab3</th></tr></thead>"));
		assertThat(
				content,
				containsString("<tr><td><a href=\"#\">Item 1</a></td><td>NONONONY</td><td>NONONONZ</td></tr>"));
	}

	@Test
	public void generateTextInputs() {
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(
				content,
				containsString("<label>FInput1<input type=\"text\" id=\"finput1\"></label>"));
		assertThat(
				content,
				containsString("<label class=\"form-group\">FInput2<select id=\"finput2\"></select></label>"));
		assertThat(
				content,
				containsString("<label title=\"document &quot;classifier&quot; according to the international standards\" class=\"form-group\">Document type<select id=\"document_type\">"
						+ documentTypeOptions() + "</select></label>"));
		assertThat(
				content,
				containsString("<label class=\"form-group col-xs-12 col-sm-6 col-md-3 col-lg-3\">Author<select id=\"author\" class=\"smartSelect\"></select></label>"));
		assertThat(
				content,
				containsString("<label>FEntrada1<input type=\"text\" id=\"fentrada1\"></label>"));
		assertThat(
				content,
				containsString("<label>Filtro entrada dois<input type=\"text\" id=\"filtro_entrada_dois\"></label>"));
		content = webInterface.getArtifacts().get(NEW_OFFICIAL_DOCUMENT)
				.getContent();
		assertThat(
				content,
				containsString("<label>Name<input type=\"text\" id=\"name\" placeholder=\"type the document's name or title\"></label>"));
		assertThat(
				content,
				containsString("<label title=\"document &quot;classifier&quot; according to the international standards\" class=\"form-group\">Document type<select id=\"document_type\">"
						+ documentTypeOptions() + "</select></label>"));
		assertThat(
				content,
				containsString("<label>Owner<input type=\"text\" id=\"owner\"></label>"));
	}

	private String documentTypeOptions() {
		String lineSep = System.getProperty("line.separator");
		return "<option>(Undefined document)</option>" + lineSep
				+ "<option value=\"int\">Internal document</option>" + lineSep
				+ "<option value=\"fed\">Federal document</option>";
	}

	@Test
	public void generateActions() {
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(
				content,
				containsString("<a href=\"one.html\"><button type=\"button\">one</button></a>"));
		assertThat(
				content,
				containsString("<a href=\"acao_alfa.html\"><button type=\"button\">Ação alfa</button></a>"));
	}

	@Test
	public void generateNumericalLabels() {
		String content = webInterface.getArtifacts().get(NEW_OFFICIAL_DOCUMENT)
				.getContent();
		assertThat(
				content,
				containsString("<span id=\"0_is_the_goal_for_the_other_customers_indicator\">0: is the goal for the 'other customers' indicator</span>"));
	}

	@Test
	public void generateDataReferences() {
		String content = webInterface.getArtifacts().get(NEW_OFFICIAL_DOCUMENT)
				.getContent();
		assertThat(content, not(containsString("${data}")));
		assertThat(content, containsString(">57</text>"));
		assertThat(content, containsString("></text>"));
	}

	@Test
	public void generateLists() {
		String content = webInterface.getArtifacts().get(NEW_OFFICIAL_DOCUMENT)
				.getContent();
		assertThat(
				content,
				containsString("<ul id=\"my_list\" aria-labelledby=\"my_list_heading\"><h2 id=\"my_list_heading\">My list</h2>"));
		assertThat(
				content,
				containsString("<li class=\"title\"><strong>Sample data</strong></li>"));
		assertThat(
				content,
				containsString("<li>See the file <em>sample-data.xml</em> to configure sample data presented here!</li>"));
		assertThat(content, not(containsString("></h2>")));
	}

	@Test
	public void generateMenuItems() throws IOException {
		String menuItem1 = "<li><a href=\"main_page.html\">Main page</a></li>";
		String menuItem2 = "<li><a href=\"new_official_document.html\">New official document</a></li>";
		String content = webInterface.getArtifacts().get(MAIN_PAGE)
				.getContent();
		assertThat(content, containsString(menuItem1));
		assertThat(content, containsString(menuItem2));
		content = webInterface.getArtifacts().get(NEW_OFFICIAL_DOCUMENT)
				.getContent();
		assertThat(content, containsString(menuItem1));
		assertThat(content, containsString(menuItem2));
	}

	@Test
	public void generateReports() {
		String content = webInterface.getReports().get(0).getContent();
		assertThat(content, containsString("<html lang=\"en\">"));
		assertThat(content, containsString("<title>WebGen report</title>"));
		assertThat(
				content,
				containsString("<thead><tr><th>Title</th><th>Data inputs</th><th>Data outputs</th></tr></thead>"));
		assertThat(
				content,
				containsString("<tr><td><a href=\"main_page.html\">Main page</a></td><td>10</td><td>10</td></tr>"));
		assertThat(
				content,
				containsString("<tr><td><a href=\"new_official_document.html\">New official document</a></td><td>4</td><td>4</td></tr>"));
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
		return new StreamUtil().extractText(getClass().getResourceAsStream(
				resourceName));
	}
}
