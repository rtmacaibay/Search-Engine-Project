import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes our entire InvertedIndex into a designated output file
 * 
 * @author Robert Macaibay
 */
public class JSONWriter {
	
	/**
	 * Writes an inputted InvertedIndex into a file at the designated file path
	 * @param index - InvertedIndex map of all the words found in certain html/htm files
	 * @param path - output file path
	 * @throws IOException - we're throwing IOExceptions because we're writing files
	 */
	public static void writeIndex(InvertedIndex index, String path) throws IOException {
		//output and writing streams so we could write output in UTF-8 encoding
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8);
		
		//try-finally block because we want to close the streams all the time
		try {
			//beginning bracket of JSON file
			writer.write("{\n");

			//grab the list of words from the InvertedIndex map
			List<String> wordList = index.copyWords();
			
			//iterate through the list of words
			for (int i = 0; i < wordList.size(); i++) {
				//grab current word
				String word = wordList.get(i);
				
				//write that word into the JSON file
				writer.write("\t\"" + word + "\": {\n");
				
				//grab the list of paths to html/htm files that the word can be found in
				List<String> pathList = index.get(word).copyPaths();
				
				//iterate through the list of paths to html/htm files 
				for (int j = 0; j < pathList.size(); j++) {
					//grab current path found
					String pathFound = pathList.get(j);
					
					//write that path found into the JSON file
					writer.write("\t\t\"" + pathFound + "\": [\n");
					
					//grab the list of positions the word appears in that html/htm file
					List<Integer> posList = index.get(word).copyPositions(pathFound);
					
					//iterate through list of positions
					for (int k = 0; k < posList.size(); k++) {
						//grab current position
						Integer pos = posList.get(k);
						
						//if we aren't at the end of the list, add a comma
						if (k < posList.size() - 1)
							writer.write("\t\t\t" + pos + ",\n");
						else
							writer.write("\t\t\t" + pos + "\n");
					}
					
					//if we aren't at the end of the list, add a comma
					if (j < pathList.size() - 1)
						writer.write("\t\t],\n");
					else
						writer.write("\t\t]\n");
				}
				
				//if we aren't at the end of the list, add a comma
				if (i < wordList.size() - 1)
					writer.write("\t},\n");
				else
					writer.write("\t}\n");
			}
			
			//write the end bracket of the JSON file
			writer.write("}");
		} finally {
			//close streams
			writer.close();
		}
	}
	
	/**
	 * Writes an inputted search results map into a file at the designated file path
	 * @param index - InvertedIndex map of all the words found in certain html/htm files
	 * @param path - output file path
	 * @throws IOException - we're throwing IOExceptions because we're writing files
	 */
	public static void writeResults(TreeMap<String, List<SearchResult>> map, String path) throws IOException {
		//output and writing streams so we could write output in UTF-8 encoding
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardCharsets.UTF_8);

		//try-finally block because we want to close the streams all the time
		try {
			//beginning bracket of JSON file
			writer.write("[\n");

			//grab the first entry of the map
			Map.Entry<String, List<SearchResult>> item = map.pollFirstEntry();

			//make sure the first entry isnt null/empty
			if (item != null) {
				//write the beginning bracket
				writer.write("\t{\n");
				//write queries label + query
				writer.write("\t\t\"queries\": ");
				writer.write("\"" + item.getKey() + "\",\n");
				//write results label
				writer.write("\t\t\"results\": [\n");
				//iterate through the results
				for (int i = 0; i < item.getValue().size(); i++) {
					//write the result output (from a helper method)
					writer.write(resultOutput(item.getValue().get(i)));
					//check if we need to pop a comma
					if (i < item.getValue().size() - 1)
						writer.write(",\n");
					else
						writer.write("\n");
				}
				//ending brackets
				writer.write("\t\t]\n");
				writer.write("\t}");
				
				//iterate through the map if there are more entries
				while ((item = map.pollFirstEntry()) != null) {
					//comma
					writer.write(",\n");
					//beginning bracket
					writer.write("\t{\n");
					//queries label + query
					writer.write("\t\t\"queries\": ");
					writer.write("\"" + item.getKey() + "\",\n");
					//results label
					writer.write("\t\t\"results\": [\n");
					//iterate through the results
					for (int i = 0; i < item.getValue().size(); i++) {
						//write the result output (from a helper method)
						writer.write(resultOutput(item.getValue().get(i)));
						//check if we need to pop a comma
						if (i < item.getValue().size() - 1)
							writer.write(",\n");
						else
							writer.write("\n");
					}
					//ending brackets
					writer.write("\t\t]\n");
					writer.write("\t}");
				}
				//new line for the end
				writer.write("\n");
			}

			//write the end bracket of the JSON file
			writer.write("]");
		} finally {
			//close streams
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
		//beginning bracket
		sb.append("\t\t\t{\n");
		//where label + path
		sb.append("\t\t\t\t\"where\": ");
		sb.append("\"" + sr.getPath().toString() + "\",\n");
		//count label + frequency of queries
		sb.append("\t\t\t\t\"count\": ");
		sb.append(sr.getFrequency() + ",\n");
		//index label + first index
		sb.append("\t\t\t\t\"index\": ");
		sb.append(sr.getPos() + "\n");
		//ending bracket
		sb.append("\t\t\t}");
		//return that
		return sb.toString();
	}
}
