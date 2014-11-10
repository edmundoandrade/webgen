package util;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Scanner;

public class TextUtil {
	public String extractText(InputStream stream) {
		Scanner scanner = new Scanner(stream, "UTF-8");
		try {
			return scanner.useDelimiter("\\Z").next();
		} finally {
			scanner.close();
		}
	}

	public String removeDiacritics(String text) {
		return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll(
				"[^\\p{ASCII}]", "");
	}
}
