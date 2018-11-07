import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Convenience class to build a {@link InvertedIndex} from a HTML file.
 * 
 * @author Robert Macaibay
 */
public class InvertedIndexBuilder {
	/**
	 * Opens the file located at the path provided, parses each line in the file
	 * into words, and stores those words in a word index.
	 *
	 * @param path - path to file to parse
	 * @param index - inverted index to add words
	 * @throws IOException - we're throwing IOExceptions because we're reading files
	 */
	public static void buildIndex(Path path, InvertedIndex index) throws IOException {
		try {
			byte[] bytes = Files.readAllBytes(path);
			String html = new String(bytes, StandardCharsets.UTF_8);

			//create a single String object of a cleaned html/htm file
			html = HTMLCleaner.stripHtml(html);

			try (BufferedReader reader = new BufferedReader(new StringReader(html));) {
				String line = null;
				//list of all the words found in a html/htm file
				List<String> allWords = new ArrayList<String>();
				
				//iterate through the String and add each word into our InvertedIndex
				while ((line = reader.readLine()) != null) {
					//parse the line, separate each word into an array
					String[] words = WordParser.parseWords(line);

					//iterate through all the words and add them individually into the InvertedIndex
					for (String word : words) {
						//somehow the parser finds empty strings(?)... so lets just ignore those
						if (word.equals(""))
							continue;
						//add the words to the list
						allWords.add(word);
					}
				}
				
				//add all the words in one fell swoop
				index.addAll(allWords.toArray(new String[allWords.size()]), path.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static class BuildTask implements Runnable {
		private InvertedIndex index;
		private Path path;
		
		public BuildTask(InvertedIndex index, Path path) {
			this.index = index;
			this.path = path;
		}

		@Override
		public void run() {
			try {
				byte[] bytes = Files.readAllBytes(path);
				String html = new String(bytes, StandardCharsets.UTF_8);

				//create a single String object of a cleaned html/htm file
				html = HTMLCleaner.stripHtml(html);

				try (BufferedReader reader = new BufferedReader(new StringReader(html));) {
					String line = null;
					//list of all the words found in a html/htm file
					List<String> allWords = new ArrayList<String>();
					
					//iterate through the String and add each word into our InvertedIndex
					while ((line = reader.readLine()) != null) {
						//parse the line, separate each word into an array
						String[] words = WordParser.parseWords(line);

						//iterate through all the words and add them individually into the InvertedIndex
						for (String word : words) {
							//somehow the parser finds empty strings(?)... so lets just ignore those
							if (word.equals(""))
								continue;
							//add the words to the list
							allWords.add(word);
						}
					}
					
					//add all the words in one fell swoop
					index.addAll(allWords.toArray(new String[allWords.size()]), path.toString());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class LinkBuildTask implements Runnable {
		private InvertedIndex index;
		private URL url;
		
		public LinkBuildTask(InvertedIndex index, URL url) {
			this.index = index;
			this.url = url;
		}

		@Override
		public void run() {
			try {
				String html = null;
				if ((html = LinkParser.fetchHtml(url)) != null) {
					//create a single String object of a cleaned html/htm file
					html = HTMLCleaner.stripHtml(html);

					try (BufferedReader reader = new BufferedReader(new StringReader(html));) {
						String line = null;
						//list of all the words found in a html/htm file
						List<String> allWords = new ArrayList<String>();

						//iterate through the String and add each word into our InvertedIndex
						while ((line = reader.readLine()) != null) {
							//parse the line, separate each word into an array
							String[] words = WordParser.parseWords(line);

							//iterate through all the words and add them individually into the InvertedIndex
							for (String word : words) {
								//somehow the parser finds empty strings(?)... so lets just ignore those
								if (word.equals(""))
									continue;
								//add the words to the list
								allWords.add(word);
							}
						}

						//add all the words in one fell swoop
						index.addAll(allWords.toArray(new String[allWords.size()]), url.toString());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
