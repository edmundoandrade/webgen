package util;

public class WebArtifact {
	private String title;
	private String content;
	private String fileName;
	private int dataInputs;
	private int dataOutputs;

	public WebArtifact(String title, String content, String fileName) {
		this.title = title;
		this.content = content;
		this.fileName = fileName;
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
}
