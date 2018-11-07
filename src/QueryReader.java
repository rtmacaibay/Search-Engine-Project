import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Gets a list of queries from a certain file
 * 
 * @author Robert Macaibay
 */
public class QueryReader {
	
	/**
	 * Gets list of queries from a specified path
	 * @param path - path to file
	 * @return list of queries
	 * @throws IOException - throwing this exception because we're reading files
	 */
	public static List<List<String>> getQueries(String path) throws IOException {
		//queries list for output
		List<List<String>> queries = new ArrayList<List<String>>();
		
		//try-with-resources block because we want to close the stream
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
			//iterate through the lines
			for (String line = reader.readLine(); line != null; line = reader.readLine()) {
				//lowercase the line, replace anything that isnt letters with a space, and trim the edges
				line = line.replaceAll("[^\\p{Alpha}]", " ").trim().toLowerCase();
				//make sure if we get a random empty string, ignore that
				if (line.equals(""))
					continue;
				//split the line query into a list
				List<String> lineQueries = Arrays.asList(line.split("\\s+"));
				//sort that list
				Collections.sort(lineQueries);
				//add it to our ouput queries list
				queries.add(lineQueries);
			}
		}
		
		//return the list
		return queries;
	}
}
