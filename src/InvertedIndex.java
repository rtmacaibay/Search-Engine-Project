import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * A nested map that holds the index of a word inside many html/htm files.
 * 
 * @author Robert Macaibay
 */
public class InvertedIndex {

	protected TreeMap<String, PathIndex> index;
	
	/**
	 * default constructor
	 */
	public InvertedIndex() {
		index = new TreeMap<>();
	}
	
	/**
	 * Adds a specific word to a specific path and position.
	 * @param word - word found
	 * @param path - path to html/htm file
	 * @param pos - position of word in that file
	 */
	public void add(String word, String path, int pos) {
		//checks if there is at least an existing PathIndex map in the map for that word
		if (index.get(word) == null) {
			//puts a new PathIndex map with the specified path and position for that word
			index.put(word, new PathIndex(path, pos));
		} else {
			//grab old PathIndex map
			PathIndex pi = index.get(word);
			//update PathIndex map
			pi.add(path, pos);
			//update InvertedIndex map
			index.put(word, pi);
		}
	}
	
	/**
	 * Adds the array of words at once, assuming the first word in the array is
	 * at position 1.
	 *
	 * @param words - array of words to add
	 * @param path - path to html/htm file
	 */
	public void addAll(String[] words, String path) {
		addAll(words, path, 1);
	}

	/**
	 * Adds the array of words at once, assuming the first word in the array is
	 * at the provided starting position
	 *
	 * @param words - array of words to add
	 * @param path - path to html/htm file
	 * @param start - starting position
	 */
	public void addAll(String[] words, String path, int start) {
		for (int i = start; i <= words.length; i++) {
			add(words[i - 1], path, i);
		}
	}
	
	/**
	 * Returns a copy of the words in this index as a sorted list.
	 * @return sorted list of paths
	 */
	public List<String> copyWords() {
		List<String> out = new ArrayList<String>();
		for (String word : index.keySet()) {
			out.add(word);
		}
		return out;
	}
	
	/**
	 * Gets the PathIndex map of a certain word
	 * @param word - specified word to find
	 * @return Mapping of paths and positions for a word
	 */
	public PathIndex get(String word) {
		return index.get(word);
	}
	
	/**
	 * Gets a list of exact search results from a list of queries
	 * @param queries - list of queries
	 * @return list of exact search results
	 */
	public List<SearchResult> exactSearch(List<String> queries) {
		//map to temporarily keep a path to search result in case of replacing previous results
		TreeMap<String, SearchResult> results = new TreeMap<String, SearchResult>();
		//list of search results for output
		List<SearchResult> out = new ArrayList<SearchResult>();
		
		//iterate through the words in the index key set
		for (String word : copyWords()) {
			//check if any of the queries contains the word
			if (queries.contains(word)) {
				//grab the path index of that word
				PathIndex pi = get(word);
				//iterate through the paths that contain that word
				for (String p : pi.copyPaths()) {
					//grab the list of positions that appeared in that path
					List<Integer> positions = pi.copyPositions(p);
					//checks if we had a previous search result of the word
					if (results.containsKey(p)) {
						//grab the previous results
						SearchResult prev = results.get(p);
						//calculate total frequency
						int totalFreq = prev.getFrequency() + positions.size();
						//check which is the earlier instance of the word
						int initPos = (positions.get(0) < prev.getPos()) ? positions.get(0) : prev.getPos();
						//create a new search result instance
						SearchResult sr = new SearchResult(totalFreq, initPos, p);
						//store that
						results.put(p, sr);
					} else {
						//create a new search result instance
						SearchResult sr = new SearchResult(positions.size(), positions.get(0), p);
						//store that
						results.put(p, sr);
					}
				}
			}
		}
		
		//iterate through our map and add it to the list
		for (String p : results.keySet())
			out.add(results.get(p));

		//sort the output list
		Collections.sort(out);
		return out;
	}
	
	/**
	 * Gets a list of partial search results from a list of queries
	 * @param queries - list of queries
	 * @return list of partial search results
	 */
	public List<SearchResult> partialSearch(List<String> queries) {
		//map to temporarily keep a path to search result in case of replacing previous results
		TreeMap<String, SearchResult> results = new TreeMap<String, SearchResult>();
		//list of search results for output
		List<SearchResult> out = new ArrayList<SearchResult>();
		
		//iterate through list of words in our index
		for (String word : copyWords()) {
			//check if there is any matches where one of the words start with a certain query
			if (queries.stream().anyMatch((s) -> word.startsWith(s))) {
				//grab the path index for that word
				PathIndex pi = get(word);
				//iterate through the positions the word is found in that path
				for (String p : pi.copyPaths()) {
					//save a list of those positions
					List<Integer> positions = pi.copyPositions(p);
					//checks if we had a previous search result of the word
					if (results.containsKey(p)) {
						//grab the previous results
						SearchResult prev = results.get(p);
						//calculate total frequency
						int totalFreq = prev.getFrequency() + positions.size();
						//check which is the earlier instance of the word
						int initPos = (positions.get(0) < prev.getPos()) ? positions.get(0) : prev.getPos();
						//create a new search result instance
						SearchResult sr = new SearchResult(totalFreq, initPos, p);
						//store that
						results.put(p, sr);
					} else {
						//create a new search result instance
						SearchResult sr = new SearchResult(positions.size(), positions.get(0), p);
						//store that
						results.put(p, sr);
					}
				}
			}
		}
		
		//iterate through our map and add it to the list
		for (String p : results.keySet())
			out.add(results.get(p));

		//sort the output list
		Collections.sort(out);
		return out;
	}
}
