import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ServletWriter {

	public static void writeResults(TreeMap<String, List<SearchResult>> map, String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8);
		
		try {
			Map.Entry<String, List<SearchResult>> item = map.pollFirstEntry();
			writer.write("\"queries\": \"" + item.getKey() + "\",\n");
			writer.write("\"results\": [\n");
			for (int i = 0; i < item.getValue().size(); i++) {
				writer.write(resultOutput(item.getValue().get(i)));
				if (i < item.getValue().size() - 1)
					writer.write(",\n");
				else
					writer.write("\n");
			}
			writer.write("]");
		} finally {
			writer.close();
		}
	}

	/**
	 * Creates a string ouput for a single search result
	 * @param sr - singular search result
	 * @return output string
	 */
	private static String resultOutput(SearchResult sr) {
		//string builder to build string output
		StringBuilder sb = new StringBuilder();
		String tabs = "&nbsp;&nbsp;&nbsp;&nbsp;";
		String link = sr.getPath().toString();
		//beginning bracket
		sb.append(tabs + "{\n");
		//where label + path
		sb.append(tabs + tabs + "\"where\": ");
		sb.append("\"<a href=\"" + link + "\" target=\"_blank\">" + link + "</a>\",\n");
		//count label + frequency of queries
		sb.append(tabs + tabs + "\"count\": ");
		sb.append(sr.getFrequency() + ",\n");
		//index label + first index
		sb.append(tabs + tabs + "\"index\": ");
		sb.append(sr.getPos() + "\n");
		//ending bracket
		sb.append(tabs + "}");
		//return that
		return sb.toString();
	}
}
