import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Driver + main method to run project. Currently calls everything needed to pass tests.
 * 
 * @author Robert Macaibay
 *
 */
public class Driver {

	/**
	 * The main method calls everything to pass functionality tests for project 1.
	 * @param args - command line arguments
	 */
	public static void main(String[] args) {
		//display command line arguments / display inputted arguments - DONT NEED THIS
		//System.out.println(Arrays.toString(args));

		//create an map out of the arguments
		ArgumentMap am = new ArgumentMap(args);
		//creates a work queue which will execute our thread tasks
		WorkQueue queue = null;

		//giant try-catch statement because we throw several IOExceptions and what not
		try {
			//check if our path argument actually exists/is viable
			checkFiles(am);

			//create an inverted index that counts the position of each word in all the html/htm files
			InvertedIndex index = new InvertedIndex();
			//number of threads... duh
			Integer threads = new Integer(1);
			//determines whether or not we're doing a query search
			boolean wantsQuery = am.hasFlag("-query") || am.hasFlag("-queryLine");
			//determines whether or not we're doing threaded work
			boolean isThreaded = am.hasFlag("-threads");
			//determines whether or not we're reading from a path
			boolean usesPath = am.hasFlag("-path");
			//determines whether or not we're reading from a url
			boolean usesUrl = am.hasFlag("-url");
			
			//check if our user wants threads
			if (isThreaded && usesPath) {
				//parse the flag value from the ArgumentMap for number of threads
				try {
					threads = Integer.parseInt(am.getString("-threads"));
				} catch (NumberFormatException e) {
					threads = new Integer(5);
				}
				
				//if the threads turn out to be less than 1, default that to 5
				if (threads < 1)
					threads = new Integer(5);
				
				//create a new work queue with that number of threads
				queue = new WorkQueue(threads);
				//also recreate our index into a NEW thread-safe inverted index
				index = new ThreadSafeInvertedIndex();
				//now we want to iterate through all the html/htm files and have our queue execute an inverted index build task
				HTMLFinder.findHtmlThreaded(am.getString("-path"), queue, index);
				//wait until the queue finishes
				queue.finish();
			} else if (usesPath) {
				//find all html/htm files designated by our path argument and also check any sub-directories.
				Path[] htmlFiles = HTMLFinder.findHtml(am.getString("-path"));
				
				//if not, build the index serially
				for (Path html : htmlFiles)
					InvertedIndexBuilder.buildIndex(html, index);
			} else if (usesUrl) {
				//create URL object
				URL url = new URL(am.getString("-url"));
				//create work queue
				queue = new WorkQueue();
				//create a thread safe inverted index
				index = new ThreadSafeInvertedIndex();
				//max amount of crawls
				Integer max = null;
				//check if user specified a limit
				try {
					max = Integer.parseInt(am.getString("-limit"));
				} catch (NumberFormatException e) {
					max = new Integer(50);
				}
				//get a preset size of max for an arraylist
				ArrayList<URL> links = new ArrayList<URL>(max);
				//parse for links
				queue.execute(new LinkParser.LinkParseTask(links, queue, url, url, max));
				queue.finish();
				//build index off those links
				for (URL u : links)
					queue.execute(new InvertedIndexBuilder.LinkBuildTask(index, u));
				queue.finish();
				queue.shutdown();
			}

			//checks if there's an "-index" flag, if so, write our inverted index results to a JSON file
			if (am.hasFlag("-index") && !am.hasFlag("-servlet"))
				JSONWriter.writeIndex(index, am.getString("-index"));


			//create a list of list of strings to hold our queries
			List<List<String>> queries = null;

			//check if there's a query flag
			if (wantsQuery) {
				if (am.hasFlag("-query"))
					queries = QueryReader.getQueries(am.getString("-query"));
				else {
					List<String> line = Arrays.asList(am.getString("-queryLine").split(" "));
					queries = new ArrayList<List<String>>();
					queries.add(line);
				}
			}
				
			//create a treemap that stores our search query and the results that came from it
			TreeMap<String, List<SearchResult>> map = new TreeMap<String, List<SearchResult>>();

			//query searching!
			if (isThreaded && wantsQuery && threads >= 1) {
				//search with threads
				querySearchWithThreads(am.hasFlag("-exact"), map, index, queries, queue);
				//wait until the queue is finished
				queue.finish();
			} else if (wantsQuery) {
				//search serially
				querySearch(am.hasFlag("-exact"), map, index, queries);
			}
			
			if (isThreaded)
				queue.shutdown(); //shutdown the queue because we don't need it
			
			if (am.hasFlag("-servlet"))
				ServletWriter.writeResults(map, am.getString("-index"));

			//if the results flag exists, write it
			if (am.hasFlag("-results"))
				JSONWriter.writeResults(map, am.getString("-results"));

		} catch (InputMismatchException | IOException e) {
			//catch any InputMismatchExceptions (I made these) and any IOExceptions
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Checks if the flag arguments have correct/created corresponding files
	 * @param am - ArgumentMap which holds our flag arguments and the corresponding file paths
	 * @throws IOException - just in case creating any files doesn't work out
	 */
	private static void checkFiles(ArgumentMap am) throws IOException {
		//check if there's an "-index" flag
		if (am.hasFlag("-index")) {
			//checks if there's a value/path for the "-index" flag and create a new file from that path
			if (am.hasValue("-index"))
				Files.createFile(Paths.get(am.getString("-index")));
		}

		//check if there's an "-results" flag
		if (am.hasFlag("-results")) {
			//checks if there's a value/path for the "-results" flag and create a new file from that path
			if (am.hasValue("-results"))
				Files.createFile(Paths.get(am.getString("-results")));
		}

		//checks if there's a path flag
		if (am.hasFlag("-path")) {
			//checks the path flag has a value/path and make sure that path exists
			//if not, throw InputMismatchExceptions (not sure if this is right) because we want a correct path
			if(!am.hasValue("-path") && !am.hasValue("-url"))
				throw new InputMismatchException("There is no specified path.");
			if (am.hasValue("-path") && !Files.exists(Paths.get(am.getString("-path"))))
				throw new InputMismatchException("Path specified isn't correct because (one or more of the following):\n1. Does not exist\n2. Is not a file or directory");
		} else if (!am.hasFlag("-url")) {
			//throw an exception because we WANT a path flag
			throw new InputMismatchException("There is no specified path.");
		}

		//checks if there's a query flag
		if (am.hasFlag("-query")) {
			//checks the query path flag has a value/path and make sure that path exists
			//if not, throw InputMismatchExceptions (not sure if this is right) because we want a correct path
			if(!am.hasValue("-query"))
				throw new InputMismatchException("There is no specified query path.");
			if (am.hasValue("-query") && !Files.exists(Paths.get(am.getString("-query"))))
				throw new InputMismatchException("Query path specified isn't correct because (one or more of the following):\n1. Does not exist\n2. Is not a file or directory");
		}
	}
	
	/**
	 * Searches through an index for any list of queries provided and saves it into a map of search results
	 * @param doExact - determines whether or not we're doing a partial or exact search
	 * @param map - map of strings which are the query lines and the search results from those lines
	 * @param index - InvertedIndex which holds a map of particular words and their locations in files
	 * @param queries - a list of query lines which are further divided into a list of individual words
	 */
	private static void querySearch(boolean doExact, TreeMap<String, List<SearchResult>> map, InvertedIndex index, List<List<String>> queries) {
		//this is for the partial search
		if (doExact) {
			//iterate through the line queries found
			for (List<String> lineQueries : queries) {
				//get the entire line
				String word = lineQueries.stream().collect(Collectors.joining(" "));
				//put the tostring output and the exact search results
				map.put(word, index.exactSearch(lineQueries));
			}
		} else { //this is for partial search
			//iterate through the line queries found
			for (List<String> lineQueries : queries) {
				//get the entire line
				String word = lineQueries.stream().collect(Collectors.joining(" "));
				//put the tostring output and the exact search results
				map.put(word, index.partialSearch(lineQueries));
			}
		}
	}
	
	/**
	 * Searches through an index for any list of queries provided and saves it into a map of search results but does it efficiently ;)
	 * @param doExact - determines whether or not we're doing a partial or exact search
	 * @param map - map of strings which are the query lines and the search results from those lines
	 * @param index - InvertedIndex which holds a map of particular words and their locations in files
	 * @param queries - a list of query lines which are further divided into a list of individual words
	 * @param queue - WorkQueue object which executes thread tasks
	 */
	private static void querySearchWithThreads(boolean doExact, TreeMap<String, List<SearchResult>> map, InvertedIndex index, List<List<String>> queries, WorkQueue queue) {
		//this is for partial search
		if (doExact) {
			//iterate through the line queries found
			for (List<String> lineQueries : queries) {
				//have the work queue execute the search task which will update our map by reference
				queue.execute(new ThreadSafeInvertedIndex.ExactSearchTask(index, map, lineQueries));
			}
		} else { //this is for exact search
			//iterate through the line queries found
			for (List<String> lineQueries : queries) {
				//have the work queue execute the search task which will update our map by reference
				queue.execute(new ThreadSafeInvertedIndex.PartialSearchTask(index, map, lineQueries));
			}
		}
	}
}
