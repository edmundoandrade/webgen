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
		webInterface = new WebInterface(
				getSpecification("/web-interface-specification.xml"), "en",
				new File("target/web-templates"), null);
		webInterface.generateArtifacts();
	}

	@Test
	public void ignoreAnyInformationBeforeFirstMark() {
		for (WebArtifact artifact : webInterface.getArtifacts().values()) {
			assertThat(artifact.getContent(),
					not(containsString("Any information")));
		}
	}

	@Test
	public void generateWebPages() {
		assertEquals(2, webInterface.getArtifacts().size());
		String content1 = webInterface.getArtifacts().get("Main page")
				.getContent();
		String content2 = webInterface.getArtifacts().get("Another page")
				.getContent();
		assertThat(content1, containsString("<html lang=\"en\">"));
		assertThat(content1, containsString("<title>Main page</title>"));
		assertThat(content2, containsString("<html lang=\"en\">"));
		assertThat(content2, containsString("<title>Another page</title>"));
	}

	@Test
	public void generateSections() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(
				content,
				containsString("<section id=\"section_a\" aria-labelledby=\"section_a_heading\"><h2 id=\"section_a_heading\">Section A</h2>"));
		assertThat(
				content,
				containsString("<section id=\"secao_x\" aria-labelledby=\"secao_x_heading\"><h2 id=\"secao_x_heading\">Seção X</h2>"));
	}

	@Test
	public void generateFilters() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(
				content,
				containsString("<form class=\"filter\" id=\"_filter\"><fieldset>"));
		assertThat(content, not(containsString("<legend></legend>")));
	}

	@Test
	public void generateTables() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(content, containsString("<table id=\"_table\">"));
		assertThat(content, not(containsString("<caption></caption>")));
	}

	@Test
	public void generateTextInputs() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(
				content,
				containsString("<label>FInput1<input type=\"text\" id=\"finput1\" value=\"\"></label>"));
		assertThat(
				content,
				containsString("<label>FInput2<input type=\"text\" id=\"finput2\" value=\"\"></label>"));
		assertThat(
				content,
				containsString("<label>Filter input three<input type=\"text\" id=\"filter_input_three\" value=\"\"></label>"));
		assertThat(
				content,
				containsString("<label>FEntrada1<input type=\"text\" id=\"fentrada1\" value=\"\"></label>"));
		assertThat(
				content,
				containsString("<label>Filtro entrada dois<input type=\"text\" id=\"filtro_entrada_dois\" value=\"\"></label>"));
	}

	@Test
	public void generateTableHeaderCells() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(
				content,
				containsString("<thead><tr><th>THeader1</th><th>THeader2</th><th>THeader3</th><th>THeader4</th></tr></thead>"));
		assertThat(
				content,
				containsString("<thead><tr><th>TCab1</th><th>TCab2</th><th>TCab3</th></tr></thead>"));
	}

	@Test
	public void generateActions() {
		String content = webInterface.getArtifacts().get("Main page")
				.getContent();
		assertThat(content,
				containsString("<a href=\"action_one.html\">Action one<a>"));
		assertThat(content,
				containsString("<a href=\"acao_alfa.html\">Ação alfa<a>"));
	}

	@Test
	public void generateReports() {
		String content = webInterface.getReports().get("WebGen report")
				.getContent();
		assertThat(content, containsString("<html lang=\"en\">"));
		assertThat(content, containsString("<title>WebGen report</title>"));
		assertThat(
				content,
				containsString("<thead><tr><th>Title</th><th>Data inputs</th><th>Data outputs</th></tr></thead>"));
		assertThat(
				content,
				containsString("<tr><td><a href=\"main_page.html\">Main page</a></td><td>9</td><td>7</td></tr>"));
		assertThat(
				content,
				containsString("<tr><td><a href=\"another_page.html\">Another page</a></td><td>0</td><td>0</td></tr>"));
	}

	@Test
	public void saveArtifactsToDir() throws IOException {
		File dir = new File("target/web-test");
		File artifactFile1 = new File(dir, "main_page.html");
		File artifactFile2 = new File(dir, "another_page.html");
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
		return new TextUtil().extractText(getClass().getResourceAsStream(
				resourceName));
	}
}
